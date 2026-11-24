# Перевірка інструкцій README виконанням — 2026-11-24

**Об'єкт:** `README.md` (коміт `f3d43f8`): команди, фрагменти конфігурації та інструкції, а також `docs/api/generate.sh`, на який посилається README.
**Виконавець:** ШІ-агент (Claude Code) на запит людини (KarasovDmytro).

## 1. Середовище

| Що | Як |
|---|---|
| Код | свіжий `git clone` репозиторію в порожній каталог (окремий клон для тестів) |
| Збирання | контейнер `eclipse-temurin:25-jdk` (OpenJDK 25.0.5.1), порожній кеш Maven: wrapper і залежності завантажувались з нуля |
| Запуск, тести | хост Linux, OpenJDK 27.0.1.1, Docker Compose v5.8.0 |
| Ізоляція | Робочий стек людини (`vet-clinic-postgres`, `vet-clinic-swagger-ui`, застосунок на 8080) не зупинявся. Перевірочний стек: `COMPOSE_PROJECT_NAME=vcreadme`, файл-перевизначення лише з іменами контейнерів, порти БД 5498/5497, Swagger UI 8082/8083, застосунок `SERVER_PORT=8091` |

Відхилення від README, яких вимагала ізоляція: інші порти й назва проєкту compose. Самі команди виконувались дослівно.

## 2. Результати

| # | Інструкція README | Результат |
|---|---|---|
| 1 | `git clone …` і `./mvnw -DskipTests package` | **Помилка, виправлено.** `./mvnw: Permission denied`: у git `mvnw` і `docs/api/generate.sh` мали режим `100644`, бо репозиторій має `core.fileMode=false`. Встановлено `100755` (`git update-index --chmod=+x`). Після цього збирання на JDK 25 з порожнім кешем завершилося `BUILD SUCCESS` за 31 с, jar `target/vet-clinic-api-0.0.1-SNAPSHOT.jar` |
| 2 | Вимога «JDK 25 або новіший» | Підтверджено на JDK 25.0.5.1 (збирання, Spotless, Checkstyle, Javadoc) і 27.0.1.1 (запуск, тести) |
| 3 | Вимога доступу до інтернету під час першого збирання | Підтверджено: wrapper завантажив Maven 3.10.0, Maven — залежності |
| 4 | `docker compose up -d` | PostgreSQL `healthy`, Swagger UI віддає сторінку й `openapi.yaml` (200), порти лише на `127.0.0.1` |
| 5 | `BOOTSTRAP_ADMIN_PASSWORD=… ./mvnw spring-boot:run` | Старт за 5,8 с, застосовано 9 міграцій Flyway (V1–V9), створено адміністратора |
| 6 | `curl … /auth/login`, потім `curl … /doctors` з токеном | 200 з `accessToken`; 200 і `[]` |
| 7 | Журнали в консоль і `var/log/` | Створено `var/log/application.log` і `var/log/audit.log` |
| 8 | Зупинка `Ctrl+C` | Перевірено сигналом SIGTERM (той самий механізм завершення JVM): `Graceful shutdown complete`, пул з'єднань закрито. Справжній Ctrl+C з терміналу не відтворювався |
| 9 | Jar: `export JWT_SECRET="$(openssl rand -base64 48)"`, `java -jar …` | Секрет 64 байти, застосунок стартує, міграції не повторюються («Current version … 9») |
| 10 | «Якщо користувач із таким email уже є, нічого не змінюється» | Підтверджено: з іншим `BOOTSTRAP_ADMIN_PASSWORD` вхід зі старим паролем — 200, з новим — 401 |
| 11 | `SERVER_PORT`, `VETCLINIC_LOGGING_DIR` | Застосунок слухає заданий порт; журнали пишуться в заданий каталог |
| 12 | `JWT_SECRET` коротший за 32 байти | Підтверджено раніше того ж дня: старт успішний, `POST /auth/login` → 500 `INTERNAL_ERROR` |
| 13 | Swagger UI проксує «Try it out» на застосунок на хості | Запит через Swagger UI дійшов до застосунку на `8080` хоста (401 без токена). **Уточнено в README:** порт `8080` зашитий у `docker/swagger-ui/default.conf.template`, тому з іншим `SERVER_PORT` проксі до застосунку не дійде |
| 14 | `.env` для `docker compose` | `SWAGGER_PORT=8083`, `DB_PORT=5497` з `.env` застосовано; `git status --ignored` показує файл як ігнорований |
| 15 | `docker compose up -d swagger-ui` (лише Swagger UI) | Запускається тільки Swagger UI |
| 16 | `docker compose down` / `down -v` | `down` зберігає том `…_postgres-data`, `down -v` його видаляє |
| 17 | Один стек на машину | **Уточнено в README:** `container_name` у `docker-compose.yaml` фіксовані, другий стек конфліктує з першим за іменами контейнерів |
| 18 | `./mvnw test` | 749 тестів, 0 падінь, 0 помилок, ~2 хв. Testcontainers працює незалежно від запущеного compose-стеку |
| 19 | `-Dtest=OpenApiContractConsistencyTest`, `-Dtest='CreateAppointmentValidationTest#*'` | Обидві команди проходять; створюються `target/test-logs`, `target/surefire-reports`, `target/openapi-check/endpoints.md` |
| 20 | `spotless:check`, `checkstyle:check`, `javadoc:javadoc javadoc:test-javadoc` | `BUILD SUCCESS`; Checkstyle: 1 попередження `VariableDeclarationUsageDistance` в `OpenApiContractConsistencyTest:382` (відоме, не блокує збирання) |
| 21 | `./docs/api/generate.sh` | **Помилка, виправлено.** `maven-javadoc-plugin` вважав старий вихід актуальним і пропускав генерацію, тож скрипт копіював застарілий HTML. Закомічений `docs/api/javadoc` відставав від `users/package-info.java` (лише перенесення рядка). Скрипт тепер очищає `target/reports/apidocs` і файл stale-data; довідник перегенеровано |
| 22 | Посилання й якорі README | Усі 10 цілей існують |

## 3. Не перевірено

- Команди для Windows cmd і PowerShell: на машині немає Windows і `pwsh`.
- Розгортання на окремому сервері та робота із зовнішнім PostgreSQL: імітовано окремою БД у контейнері на іншому порту.
- Вміст Javadoc залежить від версії JDK (наприклад, JDK 25 додає файли jQuery, JDK 27 — ні). Закомічений довідник згенеровано на JDK 27.

## 4. Підсумок

22 перевірки: 18 без зауважень, 2 помилки виправлено (біт виконання, застарілий Javadoc), 2 обмеження задокументовано в README (порт проксі Swagger UI, фіксовані імена контейнерів). Тимчасові клони, контейнери й том `vcreadme_postgres-data` видалено; робочий стек людини не змінювався.
