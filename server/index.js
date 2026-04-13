const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');
const crypto = require('crypto');
const multer = require('multer');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = 8080;
const UPLOADS_DIR = path.join(__dirname, 'uploads');
fs.mkdirSync(UPLOADS_DIR, { recursive: true });

const upload = multer({ dest: UPLOADS_DIR });

const pool = new Pool({
  connectionString: process.env.DATABASE_URL || 'postgresql://postgres:1@localhost:5432/UrokPlus',
});

/** @type {Map<string, { userId: number, expiresAt: number }>} */
const typingByChat = new Map();

function userParticipatesInChat(chatId, userId) {
  if (!userId) return false;
  const m = /^chat_(\d+)_(\d+)$/.exec(String(chatId));
  if (!m) return true;
  const a = Number(m[1]);
  const b = Number(m[2]);
  return a === userId || b === userId;
}

function peerUserIdFromChat(chatId, userId) {
  const m = /^chat_(\d+)_(\d+)$/.exec(String(chatId));
  if (!m || !userId) return null;
  const a = Number(m[1]);
  const b = Number(m[2]);
  if (a === userId) return b;
  if (b === userId) return a;
  return null;
}

async function migrateMessengerSchema() {
  const stmts = [
    `ALTER TABLE messages ADD COLUMN IF NOT EXISTS sender_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL`,
    `ALTER TABLE messages ADD COLUMN IF NOT EXISTS reply_to_id INTEGER REFERENCES messages(id) ON DELETE SET NULL`,
    `ALTER TABLE messages ADD COLUMN IF NOT EXISTS edited_at BIGINT`,
    `CREATE TABLE IF NOT EXISTS chat_read_state (
      chat_id VARCHAR(255) NOT NULL,
      user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      last_read_message_id BIGINT NOT NULL DEFAULT 0,
      updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
      PRIMARY KEY (chat_id, user_id)
    )`,
    `CREATE INDEX IF NOT EXISTS idx_chat_read_state_user ON chat_read_state(user_id)`,
    `CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_user_id)`,
    `CREATE TABLE IF NOT EXISTS assignment_student_status (
      assignment_id INTEGER NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
      student_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
      updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
      PRIMARY KEY (assignment_id, student_user_id)
    )`,
  ];
  for (const sql of stmts) {
    try {
      await pool.query(sql);
    } catch (e) {
      console.error('migrateMessengerSchema:', e.message);
    }
  }
}

app.use(cors());
app.use(express.json());
app.use('/uploads', express.static(UPLOADS_DIR));
app.set('trust proxy', true);

// Без БД — для проверки туннеля/прокси (если здесь не 200, 502 даёт не приложение, а шлюз)
app.get('/api/health', (_req, res) => {
  res.status(200).json({ ok: true, service: 'urokplus-api' });
});

function publicUploadUrl(req, storedName) {
  const proto = String(req.headers['x-forwarded-proto'] || req.protocol || 'http').split(',')[0].trim();
  const host = String(req.headers['x-forwarded-host'] || req.headers.host || `localhost:${PORT}`).split(',')[0].trim();
  return `${proto}://${host}/uploads/${storedName}`;
}

function sha256(text) {
  return crypto.createHash('sha256').update(text).digest('hex');
}

function isWeekend(dateObj) {
  const day = dateObj.getDay();
  return day === 0 || day === 6;
}

function isRussianPublicHoliday(dateObj) {
  const month = String(dateObj.getMonth() + 1).padStart(2, '0');
  const day = String(dateObj.getDate()).padStart(2, '0');
  const key = `${month}-${day}`;
  const fixedHolidays = new Set([
    '01-01', '01-02', '01-03', '01-04', '01-05', '01-06', '01-07', '01-08',
    '02-23',
    '03-08',
    '05-01',
    '05-09',
    '06-12',
    '11-04'
  ]);
  return fixedHolidays.has(key);
}

// Логирование для отладки
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} - ${req.method} ${req.url}`);
  next();
});

// --- AUTH ---
async function handleLogin(req, res) {
  try {
    const { login, password } = req.body;
    const cleanLogin = (login || '').trim();
    const hash = sha256(password || '');
    const result = await pool.query(
      'SELECT id, login, role FROM users WHERE LOWER(login) = LOWER($1) AND password_hash = $2',
      [cleanLogin, hash]
    );
    if (result.rows.length === 0) return res.status(401).json({ success: false, error: 'Неверный логин или пароль' });
    res.json({ success: true, user: result.rows[0] });
  } catch (e) { res.status(500).json({ error: e.message }); }
}

app.post('/api/auth/login', handleLogin);
app.post('/auth/login', handleLogin);
app.post('/api/auth/register', async (_, res) => res.status(403).json({ success: false, error: 'Регистрация отключена' }));
app.post('/auth/register', async (_, res) => res.status(403).json({ success: false, error: 'Регистрация отключена' }));

// --- ADMIN ---
app.post('/api/admin/register', async (req, res) => {
  try {
    const requesterId = Number(req.headers['x-user-id'] || req.query.userId);
    if (!requesterId) return res.status(401).json({ success: false, error: 'userId обязателен' });

    const requester = await pool.query(
      'SELECT role FROM users WHERE id = $1',
      [requesterId]
    );
    const requesterRole = requester.rows[0]?.role;
    if (requesterRole !== 'ADMIN') {
      return res.status(403).json({ success: false, error: 'Требуется роль ADMIN' });
    }

    const { login, password, confirm, role, name, school, grade } = req.body || {};
    const cleanLogin = String(login || '').trim();
    const newRole = String(role || '').trim().toUpperCase();
    const pw = String(password || '');
    const pwConfirm = String(confirm || '');

    if (!cleanLogin) return res.status(400).json({ success: false, error: 'login обязателен' });
    if (!pw) return res.status(400).json({ success: false, error: 'password обязателен' });
    if (pw !== pwConfirm) return res.status(400).json({ success: false, error: 'Пароли не совпадают' });

    const allowedRoles = new Set(['STUDENT', 'PARENT', 'TEACHER', 'ADMIN']);
    if (!allowedRoles.has(newRole)) return res.status(400).json({ success: false, error: 'Неверная роль' });

    const hash = sha256(pw);
    const userRes = await pool.query(
      `INSERT INTO users (login, password_hash, role)
       VALUES ($1, $2, $3)
       ON CONFLICT (login) DO NOTHING
       RETURNING id, login, role`,
      [cleanLogin, hash, newRole]
    );

    if (userRes.rows.length === 0) {
      return res.status(409).json({ success: false, error: 'Логин уже занят' });
    }

    const createdUser = userRes.rows[0];

    const safeName = String(name || '').trim() || 'Пользователь';
    const safeSchool = String(school || '').trim() || 'Школа №4';
    const safeGrade = String(grade || '').trim() || '5А';

    await pool.query(
      `INSERT INTO profile (user_id, name, school, grade, avatar, updated_at)
       VALUES ($1, $2, $3, $4, NULL, NOW())
       ON CONFLICT (user_id) DO UPDATE
       SET name = EXCLUDED.name,
           school = EXCLUDED.school,
           grade = EXCLUDED.grade,
           updated_at = NOW()`,
      [createdUser.id, safeName, safeSchool, safeGrade]
    );

    res.json({ success: true, user: createdUser });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// --- PROFILE ---
app.get('/api/profile', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || req.query.userId);
    if (!userId) return res.status(400).json({ error: 'userId обязателен' });
    const result = await pool.query('SELECT name, school, grade, avatar FROM profile WHERE user_id = $1', [userId]);
    const row = result.rows[0] || { name: 'Пользователь', school: '—', grade: '—', avatar: null };
    res.json({ name: row.name, school: row.school, grade: row.grade, avatarUrl: row.avatar });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.put('/api/profile', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || req.query.userId);
    if (!userId) return res.status(400).json({ error: 'userId обязателен' });
    const { name, school, grade, avatarUrl } = req.body || {};
    await pool.query(
      `INSERT INTO profile (user_id, name, school, grade, avatar, updated_at)
       VALUES ($1, $2, $3, $4, $5, NOW())
       ON CONFLICT (user_id) DO UPDATE
       SET name = EXCLUDED.name,
           school = EXCLUDED.school,
           grade = EXCLUDED.grade,
           avatar = EXCLUDED.avatar,
           updated_at = NOW()`,
      [userId, name || 'Пользователь', school || '—', grade || '—', avatarUrl || null]
    );
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.post('/api/profile/avatar', upload.single('avatar'), async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || req.query.userId);
    if (!userId) return res.status(400).json({ error: 'userId обязателен' });
    if (!req.file) return res.status(400).json({ error: 'Файл аватара не передан' });

    const avatarUrl = publicUploadUrl(req, req.file.filename);
    await pool.query(
      `INSERT INTO profile (user_id, name, school, grade, avatar, updated_at)
       VALUES ($1, 'Пользователь', '—', '—', $2, NOW())
       ON CONFLICT (user_id) DO UPDATE
       SET avatar = EXCLUDED.avatar,
           updated_at = NOW()`,
      [userId, avatarUrl]
    );

    await pool.query(
      `INSERT INTO uploaded_files (user_id, original_name, storage_name, file_url, mime_type, size_bytes)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [userId, req.file.originalname, req.file.filename, avatarUrl, req.file.mimetype || null, req.file.size || 0]
    );

    res.json({ success: true, avatarUrl });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// --- CHATS ---
app.get('/api/chats', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || 0);
    const result = await pool.query(
      `SELECT m.chat_id,
              MAX(m.timestamp) AS last_timestamp,
              (
                SELECT m2.text
                FROM messages m2
                WHERE m2.chat_id = m.chat_id
                ORDER BY m2.timestamp DESC
                LIMIT 1
              ) AS last_message
       FROM messages m
       GROUP BY m.chat_id
       ORDER BY last_timestamp DESC`
    );
    const chats = [];
    for (const r of result.rows) {
      if (!userParticipatesInChat(r.chat_id, userId)) continue;

      let name = r.chat_id;
      let avatarUrl = null;
      let isOnline = false;
      let peerRole = null;

      const match = /^chat_(\d+)_(\d+)$/.exec(String(r.chat_id));
      if (match && userId) {
        const id1 = Number(match[1]);
        const id2 = Number(match[2]);
        const peerId = id1 === userId ? id2 : (id2 === userId ? id1 : null);
        if (peerId) {
          const peerRes = await pool.query(
            `SELECT u.login, u.role, p.name, p.avatar
             FROM users u
             LEFT JOIN profile p ON p.user_id = u.id
             WHERE u.id = $1
             LIMIT 1`,
            [peerId]
          );
          if (peerRes.rows.length > 0) {
            const peer = peerRes.rows[0];
            name = peer.name || peer.login || `Пользователь #${peerId}`;
            avatarUrl = peer.avatar || null;
            peerRole = peer.role || null;
          }
        }
      }

      let lastRead = 0;
      if (userId) {
        const readRes = await pool.query(
          'SELECT last_read_message_id FROM chat_read_state WHERE chat_id = $1 AND user_id = $2',
          [r.chat_id, userId]
        );
        lastRead = Number(readRes.rows[0]?.last_read_message_id || 0);
      }

      const unreadRes = await pool.query(
        `SELECT COUNT(*)::int AS c FROM messages m
         WHERE m.chat_id = $1 AND m.id > $2
         AND (
           (m.sender_user_id IS NOT NULL AND m.sender_user_id <> $3)
           OR (m.sender_user_id IS NULL AND m.is_me = false)
         )`,
        [r.chat_id, lastRead, userId || -1]
      );
      const unreadCount = unreadRes.rows[0]?.c ?? 0;

      chats.push({
        id: r.chat_id,
        name,
        avatarUrl,
        isOnline,
        peerRole,
        unreadCount,
        lastMessage: r.last_message || '',
        timestamp: Number(r.last_timestamp || Date.now())
      });
    }
    res.json(chats);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get('/api/chats/:chatId/messages', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || 0);
    const { chatId } = req.params;
    if (userId && !userParticipatesInChat(chatId, userId)) {
      return res.status(403).json({ error: 'Нет доступа к чату' });
    }

    const peerId = peerUserIdFromChat(chatId, userId);
    let peerLastRead = 0;
    if (peerId) {
      const pr = await pool.query(
        'SELECT last_read_message_id FROM chat_read_state WHERE chat_id = $1 AND user_id = $2',
        [chatId, peerId]
      );
      peerLastRead = Number(pr.rows[0]?.last_read_message_id || 0);
    }

    const result = await pool.query(
      `SELECT m.id, m.chat_id, m.text, m.is_me, m.type, m.timestamp, m.sender_user_id, m.reply_to_id, m.edited_at,
              rm.text AS reply_text,
              rp.name AS reply_author_name
       FROM messages m
       LEFT JOIN messages rm ON rm.id = m.reply_to_id
       LEFT JOIN users ru ON ru.id = rm.sender_user_id
       LEFT JOIN profile rp ON rp.user_id = ru.id
       WHERE m.chat_id = $1
       ORDER BY m.timestamp ASC`,
      [chatId]
    );

    const rows = result.rows.map((r) => {
      let isMe;
      if (r.sender_user_id != null) {
        isMe = userId && Number(r.sender_user_id) === userId;
      } else {
        isMe = r.is_me;
      }

      let deliveryStatus = null;
      if (isMe && userId) {
        deliveryStatus = Number(r.id) <= peerLastRead ? 'read' : 'sent';
      }

      return {
        id: r.id,
        chatId: r.chat_id,
        text: r.text,
        isMe,
        type: r.type,
        timestamp: Number(r.timestamp),
        senderUserId: r.sender_user_id != null ? Number(r.sender_user_id) : null,
        replyToId: r.reply_to_id != null ? Number(r.reply_to_id) : null,
        replyPreviewText: r.reply_text != null ? String(r.reply_text).slice(0, 200) : null,
        replyAuthorName: r.reply_author_name || null,
        editedAt: r.edited_at != null ? Number(r.edited_at) : null,
        deliveryStatus,
      };
    });
    res.json(rows);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.post('/api/chats/:chatId/messages', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || 0);
    if (!userId) return res.status(401).json({ error: 'Требуется X-User-Id' });
    const { chatId } = req.params;
    if (!userParticipatesInChat(chatId, userId)) {
      return res.status(403).json({ error: 'Нет доступа к чату' });
    }

    const { text, type, replyToId } = req.body || {};
    const now = Date.now();

    if (replyToId) {
      const chk = await pool.query(
        'SELECT id FROM messages WHERE id = $1 AND chat_id = $2',
        [Number(replyToId), chatId]
      );
      if (chk.rows.length === 0) return res.status(400).json({ error: 'Ответ на несуществующее сообщение' });
    }

    const result = await pool.query(
      `INSERT INTO messages (chat_id, text, is_me, type, timestamp, sender_user_id, reply_to_id)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING id`,
      [chatId, text || '', true, type || 'TEXT', now, userId, replyToId ? Number(replyToId) : null]
    );
    const newId = result.rows[0].id;

    const peerId = peerUserIdFromChat(chatId, userId);
    let peerLastRead = 0;
    if (peerId) {
      const pr = await pool.query(
        'SELECT last_read_message_id FROM chat_read_state WHERE chat_id = $1 AND user_id = $2',
        [chatId, peerId]
      );
      peerLastRead = Number(pr.rows[0]?.last_read_message_id || 0);
    }
    const deliveryStatus = Number(newId) <= peerLastRead ? 'read' : 'sent';

    res.json({
      id: newId,
      chatId,
      text: text || '',
      isMe: true,
      type: type || 'TEXT',
      timestamp: now,
      senderUserId: userId,
      replyToId: replyToId ? Number(replyToId) : null,
      replyPreviewText: null,
      replyAuthorName: null,
      editedAt: null,
      deliveryStatus,
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.put('/api/chats/:chatId/messages/:messageId', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || 0);
    const { chatId, messageId } = req.params;
    const { text, type } = req.body || {};
    const now = Date.now();
    const result = await pool.query(
      `UPDATE messages
       SET text = $1, type = $2, timestamp = $3, edited_at = $4
       WHERE id = $5 AND chat_id = $6 AND sender_user_id = $7
       RETURNING id, chat_id, text, is_me, type, timestamp, sender_user_id, reply_to_id, edited_at`,
      [text || '', type || 'TEXT', now, now, Number(messageId), chatId, userId]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Сообщение не найдено или не ваше' });
    const row = result.rows[0];
    const isMe = userId && Number(row.sender_user_id) === userId;
    res.json({
      id: row.id,
      chatId: row.chat_id,
      text: row.text,
      isMe: isMe ?? row.is_me,
      type: row.type,
      timestamp: Number(row.timestamp),
      senderUserId: row.sender_user_id != null ? Number(row.sender_user_id) : null,
      replyToId: row.reply_to_id != null ? Number(row.reply_to_id) : null,
      replyPreviewText: null,
      replyAuthorName: null,
      editedAt: row.edited_at != null ? Number(row.edited_at) : null,
      deliveryStatus: isMe ? 'sent' : null,
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.post('/api/chats/:chatId/read', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || 0);
    if (!userId) return res.status(401).json({ error: 'Требуется X-User-Id' });
    const { chatId } = req.params;
    if (!userParticipatesInChat(chatId, userId)) {
      return res.status(403).json({ error: 'Нет доступа к чату' });
    }
    const lastReadMessageId = Number((req.body || {}).lastReadMessageId || 0);
    const now = Date.now();
    await pool.query(
      `INSERT INTO chat_read_state (chat_id, user_id, last_read_message_id, updated_at)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (chat_id, user_id) DO UPDATE
       SET last_read_message_id = GREATEST(chat_read_state.last_read_message_id, EXCLUDED.last_read_message_id),
           updated_at = EXCLUDED.updated_at`,
      [chatId, userId, lastReadMessageId, now]
    );
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get('/api/chats/:chatId/typing', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || 0);
    const { chatId } = req.params;
    const now = Date.now();
    const t = typingByChat.get(chatId);
    if (!t || t.expiresAt <= now) {
      typingByChat.delete(chatId);
      return res.json({ typingUserId: null });
    }
    if (userId && t.userId === userId) return res.json({ typingUserId: null });
    res.json({ typingUserId: t.userId });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.post('/api/chats/:chatId/typing', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || 0);
    if (!userId) return res.status(401).json({ error: 'Требуется X-User-Id' });
    const { chatId } = req.params;
    if (!userParticipatesInChat(chatId, userId)) {
      return res.status(403).json({ error: 'Нет доступа к чату' });
    }
    typingByChat.set(chatId, { userId, expiresAt: Date.now() + 5000 });
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.delete('/api/chats/:chatId/messages/:messageId', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || 0);
    if (!userId) return res.status(401).json({ error: 'Требуется X-User-Id' });
    const { chatId, messageId } = req.params;
    const result = await pool.query(
      `DELETE FROM messages
       WHERE id = $1 AND chat_id = $2
         AND (sender_user_id = $3 OR (sender_user_id IS NULL AND is_me = true))
       RETURNING id`,
      [Number(messageId), chatId, userId]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Сообщение не найдено' });
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get('/api/users/search', async (req, res) => {
  try {
    const query = String(req.query.query || '').trim();
    if (!query) return res.json([]);
    const result = await pool.query(
      'SELECT u.id, u.login, p.name, p.avatar as "avatarUrl", false as "isOnline", u.role FROM users u JOIN profile p ON u.id = p.user_id WHERE u.login ILIKE $1 OR p.name ILIKE $1 LIMIT 30',
      [`%${query}%`]
    );
    res.json(result.rows);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// --- GRADES ---
app.get('/api/grades', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || 0);
    const forStudentUserId = Number(req.query.studentUserId || 0) || null;

    const mapRow = (r) => ({ ...r, timestamp: Number(r.timestamp) });

    if (!Number.isFinite(userId) || userId < 1) {
      const result = await pool.query(
        'SELECT id, student_name AS "studentName", subject, grade, work_type AS "workType", timestamp, is_read AS "isRead" FROM grades ORDER BY timestamp DESC'
      );
      return res.json(result.rows.map(mapRow));
    }

    const ur = await pool.query('SELECT role FROM users WHERE id = $1', [userId]);
    const role = ur.rows[0]?.role;

    if (role === 'STUDENT') {
      const pr = await pool.query('SELECT name FROM profile WHERE user_id = $1', [userId]);
      const name = (pr.rows[0]?.name || '').trim();
      if (!name) return res.json([]);
      const result = await pool.query(
        `SELECT id, student_name AS "studentName", subject, grade, work_type AS "workType", timestamp, is_read AS "isRead"
         FROM grades WHERE TRIM(student_name) = $1
         ORDER BY timestamp DESC`,
        [name]
      );
      return res.json(result.rows.map(mapRow));
    }

    if (role === 'PARENT') {
      let names;
      if (forStudentUserId) {
        const chk = await pool.query(
          `SELECT p.name FROM parent_student ps
           INNER JOIN profile p ON p.user_id = ps.student_user_id
           WHERE ps.parent_user_id = $1 AND ps.student_user_id = $2`,
          [userId, forStudentUserId]
        );
        if (!chk.rows.length) return res.status(403).json({ error: 'Нет доступа к оценкам этого ученика' });
        names = chk.rows.map((x) => x.name.trim());
      } else {
        const lr = await pool.query(
          `SELECT TRIM(p.name) AS name FROM parent_student ps
           INNER JOIN profile p ON p.user_id = ps.student_user_id
           WHERE ps.parent_user_id = $1`,
          [userId]
        );
        names = lr.rows.map((x) => x.name).filter(Boolean);
      }
      if (!names.length) return res.json([]);
      const result = await pool.query(
        `SELECT id, student_name AS "studentName", subject, grade, work_type AS "workType", timestamp, is_read AS "isRead"
         FROM grades WHERE TRIM(student_name) = ANY($1::text[])
         ORDER BY timestamp DESC`,
        [names]
      );
      return res.json(result.rows.map(mapRow));
    }

    const result = await pool.query(
      'SELECT id, student_name AS "studentName", subject, grade, work_type AS "workType", timestamp, is_read AS "isRead" FROM grades ORDER BY timestamp DESC'
    );
    res.json(result.rows.map(mapRow));
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.post('/api/grades', async (req, res) => {
  try {
    const { studentName, subject, grade, workType, timestamp } = req.body || {};
    const result = await pool.query(
      `INSERT INTO grades (student_name, subject, grade, work_type, timestamp, is_read)
       VALUES ($1, $2, $3, $4, $5, false)
       RETURNING id, student_name AS "studentName", subject, grade, work_type AS "workType", timestamp, is_read AS "isRead"`,
      [studentName, subject, grade, workType, Number(timestamp || Date.now())]
    );
    const row = result.rows[0];
    res.json({ ...row, timestamp: Number(row.timestamp) });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// --- FILES ---
app.post('/api/files', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ error: 'Файл не передан' });
    const fileUrl = publicUploadUrl(req, req.file.filename);
    const userId = Number(req.headers['x-user-id'] || 0) || null;
    await pool.query(
      `INSERT INTO uploaded_files (user_id, original_name, storage_name, file_url, mime_type, size_bytes)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [userId, req.file.originalname, req.file.filename, fileUrl, req.file.mimetype || null, req.file.size || 0]
    );
    res.json({
      url: fileUrl,
      filename: req.file.originalname,
      size: req.file.size
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// --- ASSIGNMENTS ---
app.get('/api/assignments', async (req, res) => {
  try {
    const { date, gradeClass } = req.query;
    if (!date || !gradeClass) return res.status(400).json({ error: 'date и gradeClass обязательны' });
    const userId = Number(req.headers['x-user-id'] || 0);
    let result;
    if (Number.isFinite(userId) && userId >= 1) {
      const ur = await pool.query('SELECT role FROM users WHERE id = $1', [userId]);
      const role = ur.rows[0]?.role;
      if (role === 'STUDENT') {
        result = await pool.query(
          `SELECT a.id, a.title, a.description, a.subject, a.grade_class AS "gradeClass",
                  a.date::text AS date, a.due_date::text AS "dueDate",
                  a.attachment_url AS "attachmentUrl", a.attachment_name AS "attachmentName", a.teacher_name AS "teacherName",
                  s.status AS "myStatus"
           FROM assignments a
           LEFT JOIN assignment_student_status s ON s.assignment_id = a.id AND s.student_user_id = $3
           WHERE a.date = $1 AND a.grade_class = $2
           ORDER BY a.id DESC`,
          [date, gradeClass, userId]
        );
      } else {
        result = await pool.query(
          `SELECT id, title, description, subject, grade_class AS "gradeClass",
                  date::text AS date, due_date::text AS "dueDate",
                  attachment_url AS "attachmentUrl", attachment_name AS "attachmentName", teacher_name AS "teacherName",
                  NULL::varchar AS "myStatus"
           FROM assignments
           WHERE date = $1 AND grade_class = $2
           ORDER BY id DESC`,
          [date, gradeClass]
        );
      }
    } else {
      result = await pool.query(
        `SELECT id, title, description, subject, grade_class AS "gradeClass",
                date::text AS date, due_date::text AS "dueDate",
                attachment_url AS "attachmentUrl", attachment_name AS "attachmentName", teacher_name AS "teacherName",
                NULL::varchar AS "myStatus"
         FROM assignments
         WHERE date = $1 AND grade_class = $2
         ORDER BY id DESC`,
        [date, gradeClass]
      );
    }
    res.json(result.rows);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.put('/api/assignments/:id/status', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || 0);
    if (!userId) return res.status(401).json({ error: 'Требуется X-User-Id' });
    const ur = await pool.query('SELECT role FROM users WHERE id = $1', [userId]);
    if (ur.rows[0]?.role !== 'STUDENT') {
      return res.status(403).json({ error: 'Статус задания меняет только ученик' });
    }
    const assignmentId = Number(req.params.id);
    const status = String((req.body || {}).status || '').trim().toUpperCase();
    const allowed = new Set(['NOT_STARTED', 'IN_PROGRESS', 'DONE']);
    if (!allowed.has(status)) return res.status(400).json({ error: 'Неверный status' });
    const now = Date.now();
    await pool.query(
      `INSERT INTO assignment_student_status (assignment_id, student_user_id, status, updated_at)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (assignment_id, student_user_id) DO UPDATE
       SET status = EXCLUDED.status, updated_at = EXCLUDED.updated_at`,
      [assignmentId, userId, status, now]
    );
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get('/api/parent/children', async (req, res) => {
  try {
    const userId = Number(req.headers['x-user-id'] || 0);
    if (!userId) return res.status(401).json({ error: 'Требуется X-User-Id' });
    const ur = await pool.query('SELECT role FROM users WHERE id = $1', [userId]);
    if (ur.rows[0]?.role !== 'PARENT') {
      return res.status(403).json({ error: 'Только для родителя' });
    }
    const result = await pool.query(
      `SELECT u.id, p.name, p.grade AS "gradeClass"
       FROM parent_student ps
       INNER JOIN users u ON u.id = ps.student_user_id
       INNER JOIN profile p ON p.user_id = u.id
       WHERE ps.parent_user_id = $1
       ORDER BY p.name`,
      [userId]
    );
    res.json(result.rows);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// --- LESSONS ---
app.get('/api/lessons', async (req, res) => {
  try {
    const date = String(req.query.date || '').trim();
    const gradeClass = String(req.query.gradeClass || '').trim();
    if (!date || !gradeClass) return res.status(400).json({ error: 'date и gradeClass обязательны' });

    const dateObj = new Date(`${date}T00:00:00`);
    if (Number.isNaN(dateObj.getTime())) return res.status(400).json({ error: 'Некорректный формат date' });

    if (isWeekend(dateObj) || isRussianPublicHoliday(dateObj)) {
      return res.json([]);
    }

    const dayOfWeek = dateObj.getDay() === 0 ? 7 : dateObj.getDay();
    const result = await pool.query(
      `SELECT id, name, time, homework, teacher, grade_class AS "gradeClass", sort_order
       FROM lessons
       WHERE day_of_week = $1 AND grade_class = $2
       ORDER BY sort_order ASC, id ASC`,
      [dayOfWeek, gradeClass]
    );

    res.json(result.rows.map(r => ({
      id: r.id,
      name: r.name,
      time: r.time,
      homework: r.homework || '',
      teacher: r.teacher,
      gradeClass: r.gradeClass
    })));
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.post('/api/assignments', async (req, res) => {
  try {
    const { title, description, subject, gradeClass, date, dueDate, attachmentUrl, attachmentName, teacherName } = req.body || {};
    const result = await pool.query(
      `INSERT INTO assignments (title, description, subject, grade_class, date, due_date, attachment_url, attachment_name, teacher_name)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
       RETURNING id, title, description, subject, grade_class AS "gradeClass",
                 date::text AS date, due_date::text AS "dueDate",
                 attachment_url AS "attachmentUrl", attachment_name AS "attachmentName", teacher_name AS "teacherName"`,
      [title, description || '', subject, gradeClass, date, dueDate || null, attachmentUrl || null, attachmentName || null, teacherName || null]
    );
    res.json(result.rows[0]);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// --- SCHOOL EVENTS (лента) ---
app.get('/api/events', async (req, res) => {
  try {
    const gc = String(req.query.gradeClass || '').trim();
    let result;
    if (!gc) {
      result = await pool.query(
        `SELECT id, title, body, event_date::text AS "eventDate", grade_class AS "gradeClass"
         FROM school_events
         ORDER BY event_date DESC, id DESC
         LIMIT 50`
      );
    } else {
      result = await pool.query(
        `SELECT id, title, body, event_date::text AS "eventDate", grade_class AS "gradeClass"
         FROM school_events
         WHERE grade_class IS NULL OR grade_class = $1
         ORDER BY event_date DESC, id DESC
         LIMIT 50`,
        [gc]
      );
    }
    res.json(result.rows);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get('/api/classes', async (_req, res) => {
  try {
    const result = await pool.query(`
      SELECT DISTINCT grade_class FROM (
        SELECT grade_class FROM lessons WHERE grade_class IS NOT NULL
        UNION
        SELECT grade AS grade_class FROM profile WHERE grade IS NOT NULL AND grade <> ''
      ) q
      ORDER BY grade_class`);
    res.json(result.rows.map((r) => r.grade_class));
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get('/api/students', async (req, res) => {
  try {
    const gc = String(req.query.gradeClass || '').trim();
    if (!gc) return res.status(400).json({ error: 'gradeClass обязателен' });
    const result = await pool.query(
      `SELECT u.id, p.name, p.grade AS "gradeClass"
       FROM users u
       JOIN profile p ON p.user_id = u.id
       WHERE u.role = 'STUDENT' AND p.grade = $1
       ORDER BY p.name`,
      [gc]
    );
    res.json(result.rows);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get('/api/subjects', async (req, res) => {
  try {
    const gc = String(req.query.gradeClass || '').trim();
    if (!gc) return res.status(400).json({ error: 'gradeClass обязателен' });
    const result = await pool.query(
      `SELECT DISTINCT name FROM lessons WHERE grade_class = $1 ORDER BY name`,
      [gc]
    );
    res.json(result.rows.map((r) => ({ name: r.name })));
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get('/api/rating', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT p.name, AVG(g.grade::numeric) as average
       FROM profile p
       JOIN grades g ON p.name = g.student_name
       WHERE g.grade ~ '^[0-9]+(\\.[0-9]+)?$'
       GROUP BY p.name
       ORDER BY average DESC`
    );
    res.json(result.rows.map((r, i) => ({
      name: r.name,
      average: Number(r.average || 0).toFixed(2),
      rank: i + 1
    })));
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Тест базы
app.get('/api/db-test', async (req, res) => {
  const result = await pool.query('SELECT COUNT(*) FROM users');
  res.json({ success: true, users_count: result.rows[0].count });
});

migrateMessengerSchema().then(() => {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`API Сервер: http://localhost:${PORT} (слушает 0.0.0.0:${PORT}, проверка: GET /api/health)`);
  });
}).catch((e) => {
  console.error('migrateMessengerSchema failed', e);
  process.exit(1);
});
