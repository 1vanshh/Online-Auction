# Online-Auction

## Run database locally

1. Copy environment file:
   cp .env.example .env

2. Start PostgreSQL:
   docker compose up -d postgres

3. Check container status:
   docker compose ps

4. Stop:
   docker compose down

<span style="color: red;">Change the database user and password in .env!</span>