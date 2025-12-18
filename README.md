# ArchDrive

Приватне хмарне сховище файлів з використанням Cloudflare R2 Storage.

## Структура проєкту

```
ArchDrive/
├── backend/          # Spring Boot додаток (Java 17)
│   └── ArchDrive/
└── frontend/        # React + Vite + TypeScript додаток
```

## Вимоги

- Java 17+
- Maven 3.6+
- Node.js 18+
- npm або yarn
- Cloudflare R2 bucket з API токенами

## Налаштування Backend

### 1. Cloudflare R2 Setup

1. Створіть R2 bucket на [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. Згенеруйте API токени (Access Key ID та Secret Access Key)
3. Отримайте Account ID та Endpoint URL
4. Оновіть `backend/ArchDrive/src/main/resources/application.yml`:

```yaml
cloudflare:
  r2:
    accountId: "your-account-id"
    accessKey: "your-access-key-id"
    secretKey: "your-secret-access-key"
    bucket: "your-bucket-name"
    endpoint: "https://your-account-id.r2.cloudflarestorage.com"
    publicUrl: "" # Optional: Custom domain URL
```

Детальні інструкції дивіться у `backend/ArchDrive/R2_SETUP.md`

### 2. Запуск Backend

```bash
cd backend/ArchDrive
./mvnw spring-boot:run
```

Або через Maven:
```bash
cd backend/ArchDrive
mvn spring-boot:run
```

Backend буде доступний на `http://localhost:8080`

## Налаштування Frontend

### 1. Встановлення залежностей

```bash
cd frontend
npm install
```

### 2. Запуск Frontend

```bash
cd frontend
npm run dev
```

Frontend буде доступний на `http://localhost:5173` (або інший порт, якщо 5173 зайнятий)

## API Endpoints

### POST /api/files/upload
Завантажує файл на Cloudflare R2.

**Request:**
- Content-Type: `multipart/form-data`
- Body: `file` (файл)

**Response:**
```json
{
  "id": "uuid_filename.txt",
  "fileName": "filename.txt",
  "url": "https://r2-url/filename.txt",
  "size": 1024,
  "uploadedAt": "2024-01-01T12:00:00"
}
```

### GET /api/files
Отримує список всіх завантажених файлів.

**Response:**
```json
[
  {
    "id": "uuid_filename.txt",
    "fileName": "filename.txt",
    "url": "https://r2-url/filename.txt",
    "size": 1024,
    "uploadedAt": "2024-01-01T12:00:00"
  }
]
```

### GET /api/files/{fileName}
Завантажує файл з R2.

**Response:**
- Content-Type: `application/octet-stream`
- Body: файл як бінарні дані

### DELETE /api/files/{fileName}
Видаляє файл з R2.

**Response:**
- Status: `204 No Content`

## Особливості

- ✅ Завантаження файлів на Cloudflare R2 Storage
- ✅ Використання AWS S3 SDK v2 для роботи з R2
- ✅ REST API для роботи з файлами (upload, list, download, delete)
- ✅ Сучасний React UI з TypeScript
- ✅ Автоматичне оновлення списку файлів після завантаження/видалення
- ✅ CORS налаштований для localhost:3000 та localhost:5173

## Наступні кроки

- [ ] Додати базу даних для зберігання метаданих
- [ ] Додати автентифікацію користувачів
- [ ] Додати пагінацію для списку файлів
- [ ] Додати пошук файлів
- [ ] Додати підтримку папок/категорій

