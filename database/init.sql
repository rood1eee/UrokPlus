-- UrokPlus — скрипт инициализации БД PostgreSQL
-- Соответствует моделям приложения: users, profile, messages, grades

-- Роли пользователей (как в UserRole)
-- STUDENT, PARENT, TEACHER, ADMIN

CREATE TABLE IF NOT EXISTS users (
    id              SERIAL PRIMARY KEY,
    login           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(64) NOT NULL,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'PARENT', 'TEACHER', 'ADMIN')),
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Профиль пользователя (UserProfile), один на пользователя
CREATE TABLE IF NOT EXISTS profile (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(255) DEFAULT 'Сушков А.А.',
    school          VARCHAR(255) DEFAULT 'Школа №4',
    grade           VARCHAR(50) DEFAULT '5А',
    avatar          TEXT,
    updated_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Связь родитель → ученик (оценки и уведомления для родителя)
CREATE TABLE IF NOT EXISTS parent_student (
    parent_user_id  INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (parent_user_id, student_user_id)
);

CREATE INDEX IF NOT EXISTS idx_parent_student_parent ON parent_student(parent_user_id);

-- Сообщения чатов (Message, MessageType: TEXT, IMAGE, VOICE)
CREATE TABLE IF NOT EXISTS messages (
    id              SERIAL PRIMARY KEY,
    chat_id         VARCHAR(255) NOT NULL,
    text            TEXT NOT NULL DEFAULT '',
    is_me           BOOLEAN NOT NULL DEFAULT true,
    type            VARCHAR(20) NOT NULL DEFAULT 'TEXT' CHECK (type IN ('TEXT', 'IMAGE', 'VOICE')),
    timestamp       BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
    sender_user_id  INTEGER REFERENCES users(id) ON DELETE SET NULL,
    reply_to_id     INTEGER REFERENCES messages(id) ON DELETE SET NULL,
    edited_at       BIGINT
);

CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON messages(chat_id);
CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_user_id);

-- Последнее прочитанное сообщение в чате (для «прочитано» и счётчика непрочитанных)
CREATE TABLE IF NOT EXISTS chat_read_state (
    chat_id VARCHAR(255) NOT NULL,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_read_message_id BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
    PRIMARY KEY (chat_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_read_state_user ON chat_read_state(user_id);

CREATE TABLE IF NOT EXISTS grades (
    id              SERIAL PRIMARY KEY,
    student_name    VARCHAR(255) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    grade           VARCHAR(10) NOT NULL,
    work_type       VARCHAR(255) NOT NULL,
    timestamp       BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
    is_read         BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_grades_student_name ON grades(student_name);
CREATE INDEX IF NOT EXISTS idx_grades_timestamp ON grades(timestamp DESC);

-- Задания (Assignments) — домашние работы и задания от учителя
CREATE TABLE IF NOT EXISTS assignments (
    id              SERIAL PRIMARY KEY,
    title           TEXT NOT NULL,
    description     TEXT,
    subject         VARCHAR(255) NOT NULL,
    grade_class     VARCHAR(50) NOT NULL,
    date            DATE NOT NULL,
    due_date        DATE,
    attachment_url  TEXT,
    teacher_name    VARCHAR(255),
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_assignments_date_grade ON assignments(date, grade_class);

-- Статус выполнения ДЗ учеником (NOT_STARTED | IN_PROGRESS | DONE)
CREATE TABLE IF NOT EXISTS assignment_student_status (
    assignment_id     INTEGER NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    student_user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status            VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    updated_at        BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
    PRIMARY KEY (assignment_id, student_user_id)
);

ALTER TABLE assignments ADD COLUMN IF NOT EXISTS attachment_name TEXT;

-- События школы (лента на главной)
CREATE TABLE IF NOT EXISTS school_events (
    id              SERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    body            TEXT,
    event_date      DATE NOT NULL,
    grade_class     VARCHAR(50),
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_school_events_date ON school_events(event_date DESC);

-- Загруженные файлы (вложения в чатах/заданиях)
CREATE TABLE IF NOT EXISTS uploaded_files (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER REFERENCES users(id) ON DELETE SET NULL,
    original_name   TEXT NOT NULL,
    storage_name    TEXT NOT NULL,
    file_url        TEXT NOT NULL,
    mime_type       VARCHAR(255),
    size_bytes      BIGINT NOT NULL DEFAULT 0,
    uploaded_at     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_uploaded_files_user_id ON uploaded_files(user_id);

-- Опционально: расписание уроков (Lesson) — в приложении сейчас захардкожено по дням недели
CREATE TABLE IF NOT EXISTS lessons (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    time            VARCHAR(50) NOT NULL,
    homework        TEXT DEFAULT '',
    teacher         VARCHAR(255) NOT NULL,
    grade_class     VARCHAR(50),
    day_of_week     SMALLINT NOT NULL CHECK (day_of_week >= 1 AND day_of_week <= 7),
    sort_order      SMALLINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_lessons_day ON lessons(day_of_week);

-- Комментарии к таблицам
COMMENT ON TABLE users IS 'Пользователи приложения (ученики, родители, учителя)';
COMMENT ON TABLE profile IS 'Профиль пользователя: имя, школа, класс, аватар';
COMMENT ON TABLE parent_student IS 'Привязка аккаунта родителя к аккаунтам детей';
COMMENT ON TABLE messages IS 'Сообщения в чатах';
COMMENT ON TABLE grades IS 'Оценки и уведомления об оценках';
COMMENT ON TABLE assignments IS 'Домашние задания и задания учителей';
COMMENT ON TABLE school_events IS 'События школы (объявления, праздники, собрания)';
COMMENT ON TABLE uploaded_files IS 'Загруженные пользователями файлы';
COMMENT ON TABLE lessons IS 'Расписание уроков по дням недели';

-- ========== ТЕСТОВЫЕ ДАННЫЕ ==========
-- Пароль для всех тестовых пользователей: 123 (хэш SHA-256)

INSERT INTO users (login, password_hash, role) VALUES
  ('sushkov', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'STUDENT'),
  ('ivanov_parent', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'PARENT'),
  ('belova', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'TEACHER'),
  ('admin', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'ADMIN')
ON CONFLICT (login) DO NOTHING;

INSERT INTO profile (user_id, name, school, grade, avatar)
SELECT id, 'Сушков А.А.', 'Школа №4', '5А', NULL FROM users WHERE login = 'sushkov' LIMIT 1
ON CONFLICT (user_id) DO UPDATE SET name = EXCLUDED.name, school = EXCLUDED.school, grade = EXCLUDED.grade;

INSERT INTO profile (user_id, name, school, grade, avatar)
SELECT id, 'Иванов И.И.', 'Школа №4', '5А', NULL FROM users WHERE login = 'ivanov_parent' LIMIT 1
ON CONFLICT (user_id) DO UPDATE SET name = EXCLUDED.name, school = EXCLUDED.school, grade = EXCLUDED.grade;

INSERT INTO profile (user_id, name, school, grade, avatar)
SELECT id, 'Белова М.С.', 'Школа №4', '5А', NULL FROM users WHERE login = 'belova' LIMIT 1
ON CONFLICT (user_id) DO UPDATE SET name = EXCLUDED.name, school = EXCLUDED.school, grade = EXCLUDED.grade;

INSERT INTO profile (user_id, name, school, grade, avatar)
SELECT id, 'Администратор школы', 'Школа №4', '5А', NULL FROM users WHERE login = 'admin' LIMIT 1
ON CONFLICT (user_id) DO UPDATE SET name = EXCLUDED.name, school = EXCLUDED.school, grade = EXCLUDED.grade;

INSERT INTO messages (chat_id, text, is_me, type, timestamp) VALUES
  ('Класс 5А', 'Добрый день! Напоминаю про домашнее задание по русскому.', false, 'TEXT', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '2 hours') * 1000)::BIGINT),
  ('Класс 5А', 'Спасибо, сделаю к завтра.', true, 'TEXT', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '1 hour 50 min') * 1000)::BIGINT),
  ('Класс 5А', 'У кого вопросы по упражнению 4?', false, 'TEXT', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '30 min') * 1000)::BIGINT),
  ('Учитель Белова', 'Александр, зайди после уроков.', false, 'TEXT', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '1 day') * 1000)::BIGINT),
  ('Учитель Белова', 'Хорошо, зайду.', true, 'TEXT', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '23 hours') * 1000)::BIGINT);

INSERT INTO grades (student_name, subject, grade, work_type, timestamp, is_read) VALUES
  ('Сушков А.А.', 'Русский язык', '5', 'Домашняя работа', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '2 days') * 1000)::BIGINT, true),
  ('Сушков А.А.', 'Математика', '4', 'Контрольная работа', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '3 days') * 1000)::BIGINT, true),
  ('Сушков А.А.', 'Биология', '3', 'Ответ у доски', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '1 day') * 1000)::BIGINT, false),
  ('Сушков А.А.', 'Литература', '5', 'Сочинение', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '5 days') * 1000)::BIGINT, true),
  ('Сушков А.А.', 'Английский', '4', 'Тест Unit 4', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '4 days') * 1000)::BIGINT, true);

-- Расписание: пн/чт (1,4), вт/пт (2,5), ср (3). Как в getLessonsForDate приложения.
INSERT INTO lessons (name, time, homework, teacher, grade_class, day_of_week, sort_order) VALUES
  ('Русский язык', '8:30-9:15', 'Стр 15 упр 4', 'Белова', '5А', 1, 1),
  ('Математика', '9:25-10:10', 'Задачи 12-15', 'Иванова', '5А', 1, 2),
  ('Биология', '10:20-11:05', 'Параграф 5', 'Петров', '6Б', 1, 3),
  ('История', '11:15-12:00', 'Конспект тем', 'Романов', '5А', 1, 4),
  ('Физика', '12:10-12:55', 'Опыты', 'Кюри', '7В', 1, 5),
  ('Русский язык', '8:30-9:15', 'Стр 15 упр 4', 'Белова', '5А', 4, 1),
  ('Математика', '9:25-10:10', 'Задачи 12-15', 'Иванова', '5А', 4, 2),
  ('Биология', '10:20-11:05', 'Параграф 5', 'Петров', '6Б', 4, 3),
  ('История', '11:15-12:00', 'Конспект тем', 'Романов', '5А', 4, 4),
  ('Физика', '12:10-12:55', 'Опыты', 'Кюри', '7В', 4, 5),
  ('Английский', '8:30-9:15', 'Unit 4 exercise 2', 'Смит', '5А', 2, 1),
  ('Информатика', '9:25-10:10', 'Циклы', 'Тьюринг', '8А', 2, 2),
  ('География', '10:20-11:05', 'Карта мира', 'Козлова', '5А', 2, 3),
  ('Литература', '11:15-12:00', 'Стих наизусть', 'Белова', '5А', 2, 4),
  ('Химия', '12:10-12:55', 'Опыты', 'Менделеев', '9Б', 2, 5),
  ('Английский', '8:30-9:15', 'Unit 4 exercise 2', 'Смит', '5А', 5, 1),
  ('Информатика', '9:25-10:10', 'Циклы', 'Тьюринг', '8А', 5, 2),
  ('География', '10:20-11:05', 'Карта мира', 'Козлова', '5А', 5, 3),
  ('Литература', '11:15-12:00', 'Стих наизусть', 'Белова', '5А', 5, 4),
  ('Химия', '12:10-12:55', 'Опыты', 'Менделеев', '9Б', 5, 5),
  ('Литература', '8:30-9:15', 'Глава 2', 'Белова', '5А', 3, 1),
  ('Физкультура', '9:25-10:10', 'Форма', 'Сидоров', '5А', 3, 2),
  ('Химия', '10:20-11:05', 'Опыты', 'Менделеев', '9Б', 3, 3),
  ('Математика', '11:15-12:00', 'Задачи', 'Иванова', '5А', 3, 4);
