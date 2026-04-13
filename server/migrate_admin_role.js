const { Pool } = require("pg");

async function main() {
  const pool = new Pool({
    connectionString:
      process.env.DATABASE_URL || "postgresql://postgres:1@localhost:5432/UrokPlus",
  });

  const allowedRoles = ["STUDENT", "PARENT", "TEACHER", "ADMIN"];
  const adminLogin = "admin";
  const adminPasswordHash =
    "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"; // SHA-256("123")

  await pool.query("BEGIN");

  // На таблице `users` есть CHECK-констрейнт для роли. Удаляем его и создаем новый
  // с добавлением ADMIN.
  const cs = await pool.query(
    "SELECT conname FROM pg_constraint WHERE conrelid='users'::regclass AND contype='c'"
  );
  for (const r of cs.rows) {
    if (!/^[a-zA-Z0-9_]+$/.test(r.conname)) {
      throw new Error("Bad constraint name: " + r.conname);
    }
    await pool.query("ALTER TABLE users DROP CONSTRAINT " + r.conname);
  }

  await pool.query(
    "ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('STUDENT','PARENT','TEACHER','ADMIN'))"
  );

  await pool.query(
    "INSERT INTO users (login, password_hash, role) VALUES ($1, $2, 'ADMIN') ON CONFLICT (login) DO NOTHING",
    [adminLogin, adminPasswordHash]
  );

  await pool.query(
    `INSERT INTO profile (user_id, name, school, grade, avatar)
     SELECT id, 'Администратор школы', 'Школа №4', '5А', NULL
     FROM users
     WHERE login = $1
     ON CONFLICT (user_id) DO UPDATE
     SET name = EXCLUDED.name,
         school = EXCLUDED.school,
         grade = EXCLUDED.grade`,
    [adminLogin]
  );

  await pool.query("COMMIT");
  console.log("DB migration for ADMIN done");
  await pool.end();
}

main().catch(async (e) => {
  console.error(e);
  try {
    await new Pool().query("ROLLBACK");
  } catch {
    // ignore
  }
  process.exit(1);
});

