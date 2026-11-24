# Ветеринарна клініка - Інформаційна система

REST API для обліку власників і пацієнтів, медичних карток, розкладу прийомів і медикаментів ветеринарної клініки. Контракт API: [spec/api/openapi.yaml](spec/api/openapi.yaml), вимоги: [spec/srs.md](spec/srs.md).

Як працювати із системою адміністратору клініки та лікарю, описано в [посібнику користувача](docs/user-guide.md).

## Технології

| Компонент | Версія |
|---|---|
| Java | 25 (`java.version` у `pom.xml`) |
| Spring Boot | 4.1.2 (Web MVC, Data JPA, Security, OAuth2 Resource Server, Validation) |
| PostgreSQL | 17 (образ `postgres:17`) |
| Flyway | схема БД, міграції `V1`–`V9` у `src/main/resources/db/migration` |
| Maven | 3.10.0 через Maven Wrapper (`./mvnw`) |
| Testcontainers | PostgreSQL для інтеграційних тестів |

## Вимоги

| Що | Для чого |
|---|---|
| JDK 25 або новіший | збирання й запуск (`java -version`) |
| Docker з Compose v2 | PostgreSQL і Swagger UI (`docker compose`), Testcontainers у тестах, генерація API-довідника |
| Git | отримання коду |
| Доступ до інтернету під час першого збирання | wrapper завантажує Maven, а Maven — залежності, у `~/.m2` |

Окремо встановлювати Maven не потрібно.

## Встановлення

```bash
git clone <URL репозиторію> vet-clinic
cd vet-clinic
./mvnw -DskipTests package
```

Результат: `target/vet-clinic-api-0.0.1-SNAPSHOT.jar`, виконуваний jar із вбудованим Tomcat. Без `-DskipTests` під час збирання виконуються всі тести, тому має бути запущений Docker (розділ [Тести](#тести)). У Windows замість `./mvnw` використовуйте `mvnw.cmd`.

## Запуск

### 1. База даних

```bash
docker compose up -d
```

Команда піднімає PostgreSQL 17 і Swagger UI. База `vetclinic` слухає `127.0.0.1:5433` (користувач і пароль `vetclinic`). Дані зберігаються в томі `vet-clinic_postgres-data`. Порт 5433 вибрано, щоб не конфліктувати з локальним PostgreSQL на 5432.

Схему створювати вручну не треба: міграції Flyway застосовуються під час старту застосунку.

### 2. Застосунок

Для розробки, зі збиранням із джерел:

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

Розгортання зібраного jar (секрет обов'язково свій, див. [Конфігурація](#конфігурація)):

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
export BOOTSTRAP_ADMIN_PASSWORD='змініть-мене-8+символів'
java -jar target/vet-clinic-api-0.0.1-SNAPSHOT.jar
```

API доступний на `http://localhost:8080/api/v1`. Під час першого старту створюється адміністратор `admin@clinic.example` з паролем `BOOTSTRAP_ADMIN_PASSWORD`. Якщо користувач із таким email уже є, нічого не змінюється, тому після першого запуску змінну можна не задавати.

### 3. Перевірка

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@clinic.example","password":"змініть-мене-8+символів"}'
```

Відповідь містить `accessToken`. З ним захищені ендпоінти повертають 200:

```bash
curl -s http://localhost:8080/api/v1/doctors -H "Authorization: Bearer <accessToken>"
```

### 4. Зупинка

Застосунок зупиняється через `Ctrl+C`. Далі:

```bash
docker compose down
```

Команда `docker compose down -v` видаляє також том із даними бази.

### Swagger UI

`docker compose up -d` піднімає також Swagger UI з контрактом [spec/api/openapi.yaml](spec/api/openapi.yaml): `http://localhost:8081`. Запити «Try it out» проксуються на застосунок на хості (`localhost:8080`), тому застосунок має бути запущений. Спочатку виконайте `POST /auth/login`, скопіюйте `accessToken` із відповіді й вставте його в **Authorize**.

## Конфігурація

Застосунок налаштовується змінними оточення ([application.properties](src/main/resources/application.properties)):

| Змінна | За замовчуванням | Призначення |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `5433`, `vetclinic` | адреса бази (`DB_PORT` також задає порт у `docker-compose.yaml`) |
| `DB_USER`, `DB_PASSWORD` | `vetclinic` | облікові дані бази |
| `JWT_SECRET` | секрет лише для розробки | ключ підпису токенів HS256, не коротший за 32 байти. На проді обов'язково свій |
| `BOOTSTRAP_ADMIN_EMAIL` | `admin@clinic.example` | email першого адміністратора |
| `BOOTSTRAP_ADMIN_PASSWORD` | порожній (адміністратор не створюється) | пароль першого адміністратора, від 8 до 72 символів |
| `SERVER_PORT` | `8080` | HTTP-порт застосунку |
| `VETCLINIC_LOGGING_DIR` | `var/log` | каталог файлів журналів |
| `SWAGGER_PORT` | `8081` | порт Swagger UI (лише `docker-compose.yaml`) |

Що варто знати:

- **`JWT_SECRET`.** Якщо секрет коротший за 32 байти, застосунок запускається, але `POST /auth/login` повертає 500 `INTERNAL_ERROR`. Згенерувати секрет: `openssl rand -base64 48`.
- **Файл `.env`.** `docker compose` читає змінні з `.env` у корені проєкту (`DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SWAGGER_PORT`). Файл не комітиться (`.gitignore`). Сам застосунок `.env` не читає: змінні треба експортувати в оточення.
- **Журнали.** Пишуться в консоль і в `VETCLINIC_LOGGING_DIR`: `application.log` для журналу застосунку, `audit.log` для журналу аудиту. Обидва ротуються щодня.
- **Часовий пояс.** Застосунок сам працює в `Europe/Kyiv`, тому налаштовувати пояс сервера не потрібно.
- **Інша база.** Для зовнішнього PostgreSQL 17 задайте `DB_*` і не запускайте `docker compose` (або запустіть лише Swagger UI: `docker compose up -d swagger-ui`).

## API-довідник

[docs/api/index.md](docs/api/index.md) містить таблицю всіх ендпоінтів із посиланнями на статичний довідник REST API ([docs/api/rest/index.html](docs/api/rest/index.html), Redoc з `openapi.yaml`) і на Javadoc ([docs/api/javadoc/index.html](docs/api/javadoc/index.html)). Щоб оновити обидва довідники, потрібні Docker і JDK:

```bash
./docs/api/generate.sh
```

## Тести

```bash
./mvnw test
```

Тести самі піднімають PostgreSQL через Testcontainers (потрібен запущений Docker) і не залежать від `docker-compose.yaml`. Журнали застосунку під час тестів пишуться в `target/test-logs`, звіти — в `target/surefire-reports`.

Окремий тестовий клас або метод:

```bash
./mvnw test -Dtest=OpenApiContractConsistencyTest
```

```bash
./mvnw test -Dtest='CreateAppointmentValidationTest#*'
```

`OpenApiContractConsistencyTest` звіряє ендпоінти застосунку з `spec/api/openapi.yaml` і записує таблицю в `target/openapi-check/endpoints.md`.

### Перевірки якості коду

| Команда | Що перевіряє |
|---|---|
| `./mvnw spotless:check` | форматування за Google Java Style (`./mvnw spotless:apply` виправляє) |
| `./mvnw checkstyle:check` | правила `google_checks.xml` для `src/main` і `src/test` |
| `./mvnw javadoc:javadoc javadoc:test-javadoc` | синтаксис Javadoc (doclint), збірка падає на попередженнях |
