-- UrokPlus: полное наполнение тестовыми данными
-- База: PostgreSQL, схема public
-- Пароль для всех пользователей: 123
-- SHA-256("123") = a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3

BEGIN;

-- Очистка данных (без удаления таблиц)
TRUNCATE TABLE uploaded_files RESTART IDENTITY CASCADE;
TRUNCATE TABLE messages RESTART IDENTITY CASCADE;
TRUNCATE TABLE grades RESTART IDENTITY CASCADE;
TRUNCATE TABLE assignments RESTART IDENTITY CASCADE;
TRUNCATE TABLE school_events RESTART IDENTITY CASCADE;
TRUNCATE TABLE lessons RESTART IDENTITY CASCADE;
TRUNCATE TABLE parent_student RESTART IDENTITY CASCADE;
TRUNCATE TABLE profile RESTART IDENTITY CASCADE;
TRUNCATE TABLE users RESTART IDENTITY CASCADE;

-- Пользователи
INSERT INTO users (login, password_hash, role) VALUES
  ('student_5a', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'STUDENT'),
  ('parent_5a',  'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'PARENT'),
  ('teacher_ru', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'TEACHER'),
  ('teacher_math','a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'TEACHER'),
  ('student_6b', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'STUDENT'),
  ('admin', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'ADMIN');

-- Профили
INSERT INTO profile (user_id, name, school, grade, avatar)
SELECT id, 'Сушков Александр', 'Школа №4', '5А', NULL FROM users WHERE login = 'student_5a';
INSERT INTO profile (user_id, name, school, grade, avatar)
SELECT id, 'Иванова Марина (мама)', 'Школа №4', '5А', NULL FROM users WHERE login = 'parent_5a';
INSERT INTO profile (user_id, name, school, grade, avatar)
SELECT id, 'Белова Мария Сергеевна', 'Школа №4', '5А', NULL FROM users WHERE login = 'teacher_ru';

INSERT INTO profile (user_id, name, school, grade, avatar)
SELECT id, 'Администратор школы', 'Школа №4', '5А', NULL FROM users WHERE login = 'admin';
INSERT INTO profile (user_id, name, school, grade, avatar)
SELECT id, 'Иванова Ольга Петровна', 'Школа №4', '5А', NULL FROM users WHERE login = 'teacher_math';
INSERT INTO profile (user_id, name, school, grade, avatar)
SELECT id, 'Петров Даниил', 'Школа №4', '6Б', NULL FROM users WHERE login = 'student_6b';

-- Родитель видит оценки ребёнка (GET /api/grades для роли PARENT)
INSERT INTO parent_student (parent_user_id, student_user_id)
SELECT pu.id, su.id
FROM users pu
CROSS JOIN users su
WHERE pu.login = 'parent_5a' AND su.login = 'student_5a'
ON CONFLICT DO NOTHING;

-- Сообщения чатов
INSERT INTO messages (chat_id, text, is_me, type, timestamp) VALUES
  ('Класс 5А', 'Доброе утро! Сегодня проверка домашнего задания.', false, 'TEXT', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '6 hours') * 1000)::BIGINT),
  ('Класс 5А', 'Принято, подготовились.', true, 'TEXT', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '5 hours 50 minutes') * 1000)::BIGINT),
  ('Класс 5А', 'После 3 урока консультация по математике.', false, 'TEXT', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '2 hours') * 1000)::BIGINT),
  ('Учитель Белова', 'Саша, напиши мне после уроков.', false, 'TEXT', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '1 day') * 1000)::BIGINT),
  ('Учитель Белова', 'Хорошо, Мария Сергеевна.', true, 'TEXT', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '23 hours') * 1000)::BIGINT),
  ('Родители 5А', 'Собрание в пятницу в 18:00.', false, 'TEXT', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '3 days') * 1000)::BIGINT),
  ('Родители 5А', 'Спасибо за информацию!', true, 'TEXT', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '2 days 22 hours') * 1000)::BIGINT);

-- Оценки
INSERT INTO grades (student_name, subject, grade, work_type, timestamp, is_read) VALUES
  ('Сушков Александр', 'Русский язык', '5', 'Домашняя работа', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '2 days') * 1000)::BIGINT, true),
  ('Сушков Александр', 'Математика', '4', 'Контрольная работа', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '3 days') * 1000)::BIGINT, true),
  ('Сушков Александр', 'Литература', '5', 'Сочинение', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '5 days') * 1000)::BIGINT, true),
  ('Сушков Александр', 'Английский', '4', 'Тест Unit 4', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '4 days') * 1000)::BIGINT, false),
  ('Сушков Александр', 'История', '5', 'Ответ у доски', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '1 day') * 1000)::BIGINT, false),
  ('Петров Даниил', 'Биология', '5', 'Практическая работа', (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '2 days') * 1000)::BIGINT, true);

-- Задания (на даты для теста календаря)
INSERT INTO assignments (title, description, subject, grade_class, date, due_date, attachment_url, teacher_name) VALUES
  ('Упр. 24', 'Русский язык, стр. 41, упражнения 24-25', 'Русский язык', '5А', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 day', NULL, 'Белова М.С.'),
  ('Подготовка к контрольной', 'Повторить темы: дроби, проценты', 'Математика', '5А', CURRENT_DATE, CURRENT_DATE + INTERVAL '2 day', NULL, 'Иванова О.П.'),
  ('Чтение рассказа', 'Прочитать рассказ и ответить на вопросы', 'Литература', '5А', CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE + INTERVAL '3 day', NULL, 'Белова М.С.'),
  ('Лабораторная №2', 'Подготовить тетрадь и отчет', 'Биология', '6Б', CURRENT_DATE, CURRENT_DATE + INTERVAL '2 day', NULL, 'Петров А.В.');

-- События школы (лента)
INSERT INTO school_events (title, body, event_date, grade_class) VALUES
  ('Родительское собрание', 'Класс 5А, актовый зал, 18:00. Повестка: итоги четверти.', CURRENT_DATE + INTERVAL '3 day', '5А'),
  ('День самоуправления', 'Короткий день, уроки по сокращённому расписанию.', CURRENT_DATE + INTERVAL '7 day', NULL),
  ('Олимпиада по математике', 'Регистрация у классного руководителя до пятницы.', CURRENT_DATE + INTERVAL '10 day', NULL),
  ('Фотодень', 'Принести форму для классного фото.', CURRENT_DATE + INTERVAL '1 day', '5А');

-- Расписание уроков
INSERT INTO lessons (name, time, homework, teacher, grade_class, day_of_week, sort_order) VALUES
  ('Русский язык', '8:30-9:15', 'Стр 41 упр 24', 'Белова М.С.', '5А', 1, 1),
  ('Математика', '9:25-10:10', 'Задачи 12-15', 'Иванова О.П.', '5А', 1, 2),
  ('История', '10:20-11:05', 'Параграф 6', 'Романов И.А.', '5А', 1, 3),
  ('Литература', '11:15-12:00', 'Стих наизусть', 'Белова М.С.', '5А', 1, 4),
  ('Английский', '8:30-9:15', 'Unit 4 exercise 2', 'Смит Д.', '5А', 2, 1),
  ('Информатика', '9:25-10:10', 'Циклы и условия', 'Тьюринг А.', '5А', 2, 2),
  ('География', '10:20-11:05', 'Карта материков', 'Козлова Е.', '5А', 2, 3),
  ('Биология', '11:15-12:00', 'Параграф 5', 'Петров А.В.', '5А', 3, 1),
  ('Физкультура', '12:10-12:55', 'Спортивная форма', 'Сидоров П.', '5А', 3, 2),
  ('Русский язык', '8:30-9:15', 'Словарные слова', 'Белова М.С.', '5А', 4, 1),
  ('Математика', '9:25-10:10', 'Повторить формулы', 'Иванова О.П.', '5А', 4, 2),
  ('Классный час', '10:20-11:05', 'Подготовить вопросы', 'Куратор', '5А', 5, 1);

COMMIT;
