# UrokPlus API

Сервер API и страница документации для приложения UrokPlus.

## Требования

- Node.js 18+
- PostgreSQL с применённым скриптом `database/init.sql`

## Запуск

```bash
cd server
npm install
npm start
```

По умолчанию сервер слушает порт **3000**. Документация API: **http://localhost:3000/**

**Доступ с телефона через ngrok:** запустите сервер (`npm start`), затем в другом терминале: `ngrok http 3000`. В приложении должен быть прописан URL туннеля (например `https://....ngrok-free.dev`). Если туннель был на порт 80 — перезапустите ngrok с портом 3000, иначе будет 502 Bad Gateway.

## Переменные окружения

| Переменная       | Описание |
|------------------|----------|
| `PORT`           | Порт сервера (по умолчанию 3000) |
| `DATABASE_URL`   | Строка подключения PostgreSQL, например: `postgresql://user:password@localhost:5432/urokplus` |

Пример для Windows (PowerShell):

```powershell
$env:DATABASE_URL = "postgresql://postgres:postgres@localhost:5432/urokplus"
npm start
```

## Эндпоинты

- `GET /api/health` — проверка работы и подключения к БД
- `POST /api/auth/register` — регистрация
- `POST /api/auth/login` — вход
- `GET /api/profile`, `PUT /api/profile` — профиль пользователя
- `GET /api/chats/:chatId/messages`, `POST /api/chats/:chatId/messages` — сообщения чата
- `GET /api/grades`, `POST /api/grades` — оценки
- `GET /api/lessons?day=1` — расписание по дню недели (1–7)

Подробнее — на главной странице после запуска сервера.
