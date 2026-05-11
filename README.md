# Online Auction

Online Auction — это веб-приложение для проведения онлайн-аукционов.  
Пользователи могут просматривать лоты без регистрации, а зарегистрированные пользователи могут создавать лоты и делать ставки.  
Администратор может управлять пользователями, лотами, категориями и результатами аукционов.

## Основные возможности

- Регистрация и авторизация пользователей
- JWT-аутентификация
- Просмотр лотов без регистрации
- Создание лотов с категорией, стартовой ценой, шагом ставки, временем завершения и фото
- Загрузка фото лота с компьютера
- Сортировка и фильтрация лотов по цене, категории и статусу
- Участие в торгах только для авторизованных пользователей
- Определение победителя после завершения аукциона
- Бан пользователя на 7 дней, если победитель не оплатил лот
- Админ-панель для управления пользователями, лотами и категориями
- Сохранение данных в PostgreSQL
- Микросервисная архитектура
- Backend и frontend тесты
- CI через GitHub Actions

## Архитектура

Проект состоит из нескольких сервисов:

- `auth-service` — регистрация, логин, JWT, пользователи и роли
- `auction-service` — лоты, категории, фото лотов
- `bidding-service` — ставки, результаты торгов, победители
- `frontend/service` — пользовательский интерфейс
- `nginx` — reverse proxy для frontend и backend API
- `postgres` — база данных

## Технологии

### Backend

- Java
- Spring Boot
- Spring Security
- JWT
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven

### Frontend

- React
- Vite
- JavaScript
- CSS

### Infrastructure

- Docker
- Docker Compose
- Nginx
- GitHub Actions


## Команды для запуска и работы с проектом

### Проверка установленных инструментов

```bash
docker --version
docker compose version
git --version
```

---

### Клонирование проекта

```bash
git clone https://github.com/1vanshh/Online-Auction.git
cd Online-Auction
```

---

### Создание `.env` файла

#### Linux / macOS

```bash
cp .env.example .env
```

#### Windows PowerShell

```powershell
Copy-Item .env.example .env
```

---

### Пример содержимого `.env`

```env
POSTGRES_DB=online_auction
POSTGRES_USER=auction_user
POSTGRES_PASSWORD=auction_password

JWT_SECRET=your-super-secret-jwt-key
JWT_ACCESS_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000
```

---

### Запуск проекта

```bash
docker compose up --build
```

---

### Запуск проекта в фоне

```bash
docker compose up --build -d
```

---

### Остановка проекта

```bash
docker compose down
```

---

### Остановка проекта с удалением данных БД и загруженных фото

```bash
docker compose down -v
```

---


### Выдача роли администратора пользователю

```bash
docker compose exec postgres psql -U auction_user -d online_auction -c "UPDATE auth.users SET role = 'ADMIN' WHERE email = 'admin@admin.com';"
```
