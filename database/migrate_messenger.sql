-- Мессенджер: отправитель, ответы, прочтение (применить к существующей БД)
-- PostgreSQL 11+

ALTER TABLE messages ADD COLUMN IF NOT EXISTS sender_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS reply_to_id INTEGER REFERENCES messages(id) ON DELETE SET NULL;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS edited_at BIGINT;

CREATE TABLE IF NOT EXISTS chat_read_state (
    chat_id VARCHAR(255) NOT NULL,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_read_message_id BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
    PRIMARY KEY (chat_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_read_state_user ON chat_read_state(user_id);
