# OPENAPI-CHECK: журнал валідації специфікації та перевірки mock-сервера

**Дата:** 2026-10-29
**Тип артефакту:** Журнал контрольних перевірок (SDD Logging, SRS §6.4)
**Контекст:** Кроки 9 і 10 проєктування API: автоматична валідація `spec/api/openapi.yaml` і запуск mock-сервера без серверної реалізації. Перевірки виконав ШІ-агент (Claude Code, Sonnet 5) на запит людини (KarasovDmytro).

---

## 1. Об'єкт перевірки

- `spec/api/openapi.yaml`: OpenAPI 3.0.3, 32 операції, схеми та єдина модель помилок за Addendum 001 і Addendum 002.

## 2. Валідація специфікації

| Інструмент | Версія / джерело | Команда | Результат |
|---|---|---|---|
| Redocly CLI | `@redocly/cli@latest` (npx) | `redocly lint spec/api/openapi.yaml` | Валідна, 0 помилок, 1 попередження: `info-license` (немає поля `license`) |
| Spectral | `@stoplight/spectral-cli@latest` (npx), набір правил `spectral:oas` | `spectral lint spec/api/openapi.yaml --ruleset .spectral.yaml` | 0 помилок, 1 попередження: `info-contact` (немає об'єкта `contact`) |

Попередження про `license` і `contact` не стосуються коректності контракту. Значення не вигадувалися: їх має задати людина.

Під час першої спроби Redocly виявив дві помилки, виправлені до фінального запуску:
1. Описи з двокрапкою в незаквоченому значенні YAML (8 місць) не розбиралися: описи взято в лапки.
2. `nullable: true` у `Slot.appointment` без `type`: додано `type: object`.

Покриття вимог: 39 з 41 REQ-ID присутні в `x-requirements`; REQ-F-004 і REQ-NF-002 вказані в `x-cross-cutting-requirements` як наскрізні. Зайвих REQ-ID, яких немає в SRS, немає. Кількість `operationId`: 32, дублікатів немає.

## 3. Mock-сервер

- Інструмент: Stoplight Prism 5.16.0 (`npx @stoplight/prism-cli mock spec/api/openapi.yaml -p 4010 -h 127.0.0.1`). Prism заявляє `engines.node >= 24.18`, на Node 22.19 запустився з попередженнями без помилок.
- Серверна реалізація не писалася: відповіді формуються з прикладів і схем специфікації.
- Prism ігнорує відносний `servers.url: /api/v1`, тому маршрути доступні без префікса (`http://127.0.0.1:4010/doctors`).

### Результати запитів

| # | Запит | Очікування | Фактично |
|---|---|---|---|
| 1 | `GET /doctors` без токена | 401 | 401 |
| 2 | `GET /medications` із токеном | 200, сторінка медикаментів | 200, структура `{page, pageSize, total, items}` |
| 3 | `POST /appointments`, валідне тіло | 201 | 201, `status: SCHEDULED` |
| 4 | `POST /appointments`, `Prefer: code=409` | 409 `SLOT_TAKEN` | 409, `SLOT_TAKEN`, «Слот вже зайнятий» (BDD §9.2) |
| 5 | `POST /appointments/{id}/visit`, `Prefer: code=409` | 409 `INSUFFICIENT_STOCK` | 409, повідомлення «Недостатній залишок: Новокаїн (доступно: 5, запитано: 8)» (BDD §9.2) |
| 6 | `POST /appointments` без `startsAt` і з зайвим полем | 422 | 422 |
| 7 | `DELETE /patients/{id}` | 405 (REQ-F-016) | 405 |
| 8 | `PATCH /patients/{id}` з `ownerId` | 422 (REQ-F-014) | 422 |
| 9 | `GET /doctors/{id}/schedule` без `from`, `to` | 422 | 422 |

## 4. Обмеження перевірки та виявлені зауваження

- Mock перевіряє контракт, а не бізнес-логіку: конфлікт слота, нестача медикаментів, ролі (403) відтворюються лише через заголовок `Prefer: code=…`, самі правила не виконуються. Їх перевірятимуть тести TSK-003…TSK-013.
- Тіла відповідей 401 і 422 формувалися з прикладів і схем. Для 401 та більшості 409 прикладів не було, тому Prism і Postman повертали шаблонні або випадкові значення (`code: MALFORMED_REQUEST`, `status: 5183`, `status: CANCELLED` при створенні запису). Усунуто в розділі 5.
- Відповідь 405 для `DELETE` формує сам Prism у власному форматі, а не в моделі `Error`. Реальний сервер має повертати `Error` з кодом `METHOD_NOT_ALLOWED` (AD-18).
- Контрактних тестів на відповідність реалізації специфікації ще немає (етап TDD, TSK-003…TSK-013).

## 5. Повторна перевірка після додавання прикладів та перевірка в Postman

**Postman mock (запуск людиною).** Колекція імпортується зі специфікації коректно (9 папок за тегами), коди стану відповідають операціям (200, 401, 201). Виявлено два обмеження, що не стосуються контракту:
1. Для відповідей без явного прикладу Postman генерує випадкові значення зі схеми (`page: 6616` при запиті `page=1`; `code: EMAIL_TAKEN`, `status: 5183` для 401; `status: CANCELLED` і `endsAt` у 2018 році для створеного запису).
2. Postman mock не перевіряє тіло запиту за схемою: `PATCH /patients/{id}` з полем `ownerId` повернув 200 замість 422 (REQ-F-014).

Тому для протоколу обрано Prism, який перевіряє запити й повертає приклади зі специфікації.

**Усунення.** До `spec/api/openapi.yaml` додано іменовані приклади для кожного коду помилки (18 нових у `components/examples`, 20 відповідей посилаються на них; разом із наявними `SlotTakenError`, `InsufficientStockError`, `ValidationErrorExample`), а також `example` для схем `Appointment` і `MedicationPage`.

**Повторна валідація:** Redocly: valid (1 попередження `info-license`); Spectral: 0 помилок (1 попередження `info-contact`).

**Повторні запити до Prism** (тестовий екземпляр на порту 4011, після перевірки зупинений):

| Запит | Результат |
|---|---|
| `GET /doctors` без токена | 401, `UNAUTHORIZED`, «Потрібна автентифікація» |
| `GET /medications` | 200, `page: 1`, `pageSize: 20`, `total: 2`, два препарати |
| `POST /appointments` | 201, `status: SCHEDULED`, `endsAt` = `startsAt` + 30 хв, `visitId: null` |
| `POST /appointments` + `Prefer: code=409, example=slotTaken` | 409, `SLOT_TAKEN`, «Слот вже зайнятий» |
| `POST /appointments/{id}/visit` + `Prefer: code=409, example=insufficientStock` | 409, `INSUFFICIENT_STOCK`, «Недостатній залишок: Новокаїн (доступно: 5, запитано: 8)» |
| `POST /appointments/{id}/visit` + `Prefer: code=409` без `example` | 409, перший приклад зі списку (`APPOINTMENT_NOT_SCHEDULED`): для вибору конкретного коду потрібен `example=` |
| `POST /appointments` з зайвим полем | 422 |
| `PATCH /patients/{id}` з `ownerId` | 422 |
| `DELETE /patients/{id}` | 405 |

**Збій запуску (1 раз).** Команда `prism mock … -p 4010` у терміналі людини завершилася з `EADDRINUSE`: порт займав фоновий екземпляр Prism з першої перевірки (PID 2808, команда збігалася). Процес зупинено, порт 4010 звільнено. Спроба запуску Prism у панелі Terminal застосунку не вдалася (оболонка не дійшла до запиту за 60 с), тому перевірку виконано фоновим екземпляром.

## 6. Позитивні сценарії в OpenAPI

До `spec/api/openapi.yaml` додано іменовані приклади основних позитивних сценаріїв: для 13 ключових операцій приклад запиту (`requestBody.examples.valid`, де є тіло) і успішної відповіді (`responses.2xx.examples.success`), а також приклади для `getAppointment`, `listAppointments`, `listMedications`. Усього 24 нові приклади в `components/examples`. Дані пов'язані між собою й узгоджені з BDD §9.2: лікар Іваненко О.П., власниця Коваленко М.І., пацієнт Барсик, Амоксицилін (залишок 10 → 7 → 12) і Новокаїн. Наскрізний сценарій описано в `info.description`: `login` → `createUser` → `createOwner` → `createPatient` → `getDoctorSchedule` → `createAppointment` → `rescheduleAppointment`/`cancelAppointment` → `createVisit` → `listPatientVisits` → `adjustMedicationStock` → `listNotifications`.

**Виявлено та виправлено під час додавання.**
1. Spectral (`oas3-valid-media-example`) і Redocly (`no-invalid-media-type-examples`) показали, що приклад слота з `appointment: null` не відповідає схемі: вкладена схема `SlotAppointment` не допускала `null`. Додано `nullable: true` до `SlotAppointment`. Це був справжній дефект схеми (вільні слоти мали б порушувати контракт).
2. Prism віддає `example` зі схеми раніше за іменований приклад відповіді: `POST …/cancel` повернув `SCHEDULED` замість `CANCELLED`, `PATCH` запису повернув старий час. Допоміжні `example` у схемах `Appointment` і `MedicationPage` видалено, їхній вміст перенесено в іменовані приклади `MedicationPageOk`, `AppointmentPageOk`.

**Повторна перевірка.** Redocly: valid (1 попередження `info-license`); Spectral: 0 помилок (1 попередження `info-contact`); усі приклади відповідають схемам. Prism (порт 4012, зупинено): `GET /medications` 200 (`page 1`, `pageSize 20`, `total 2`), `POST /appointments` 201 (`SCHEDULED`, `endsAt` = `startsAt` + 30 хв, `patientId` Барсика), перенесення 200 (15:00–15:30), скасування 200 (`CANCELLED`), `GET /doctors` без токена 401 `UNAUTHORIZED`, `POST …/visit` 201, 409 `SLOT_TAKEN` (`example=slotTaken`), 422, 405.

**Вплив на скріни протоколу.** У відповіді запиту D (`POST /appointments`) `patientId` тепер `2b6e4a80-…` (раніше `9e1a6c24-…`, що збігалося з id Новокаїну); скрін D варто перезняти. Решта (A, B, C, E, F, G, H, I) не змінилися.

## 7. Відкриті питання

- Додати `license` і `contact` в `info` (попередження лінтерів).
- Чи задавати абсолютний `servers.url` для середовища розробки, щоб mock віддавав шляхи з префіксом `/api/v1`.
