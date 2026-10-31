# Ветеринарна клініка - Інформаційна система

## Запуск

Потрібні Docker (Compose v2) і JDK 25. Maven встановлювати не треба, працює `./mvnw`.

1. Підняти PostgreSQL 17:

   ```bash
   docker compose up -d
   ```

   База `vetclinic` слухає `127.0.0.1:5433` (користувач і пароль `vetclinic`). Дані зберігаються в томі `vet-clinic_postgres-data`; `docker compose down -v` видаляє їх разом із томом.

2. Запустити застосунок (міграції Flyway застосовуються під час старту, перший адміністратор створюється з `BOOTSTRAP_ADMIN_PASSWORD`):

   ```bash
   BOOTSTRAP_ADMIN_PASSWORD='змініть-мене-8+символів' ./mvnw spring-boot:run
   ```

   Те саме у Windows cmd:

   ```bat
   set "BOOTSTRAP_ADMIN_PASSWORD=змініть-мене-8+символів" && mvnw.cmd spring-boot:run
   ```

   І у PowerShell:

   ```powershell
   $env:BOOTSTRAP_ADMIN_PASSWORD = 'змініть-мене-8+символів'; .\mvnw.cmd spring-boot:run
   ```

   API доступний на `http://localhost:8080/api/v1`, вхід: `POST /auth/login` з `admin@clinic.example` та вказаним паролем. Журнали пишуться в консоль і в `var/log/`.

### Swagger UI

`docker compose up -d` піднімає також Swagger UI з контрактом [spec/api/openapi.yaml](spec/api/openapi.yaml): `http://localhost:8081`. Запити «Try it out» проксуються на застосунок на хості (`localhost:8080`), тому застосунок має бути запущений. Спочатку виконайте `POST /auth/login`, скопіюйте `accessToken` із відповіді й вставте його в **Authorize**.

### Налаштування середовища

| Змінна | За замовчуванням | Призначення |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `5433`, `vetclinic` | адреса бази (`DB_PORT` також задає порт у `docker-compose.yaml`) |
| `DB_USER`, `DB_PASSWORD` | `vetclinic` | облікові дані бази |
| `SWAGGER_PORT` | `8081` | порт Swagger UI |
| `JWT_SECRET` | лише для розробки | ключ підпису токенів, на проде обов'язково змінити |
| `BOOTSTRAP_ADMIN_EMAIL` | `admin@clinic.example` | email першого адміністратора |
| `BOOTSTRAP_ADMIN_PASSWORD` | порожній (адміністратора не створюється) | пароль першого адміністратора |

Порт 5433 вибрано, щоб не конфліктувати з локальним PostgreSQL на 5432.

## Тести

```bash
./mvnw test
```

Тести самі піднімають PostgreSQL через Testcontainers (потрібен запущений Docker) і не залежать від `docker-compose.yaml`.
