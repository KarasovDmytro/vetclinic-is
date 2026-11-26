# Аудит термінології коду щодо глосарію — 2026-11-24

## 1. Джерело глосарію

- **Основний глосарій:** [`spec/srs.md` §3 «Терміни та визначення»](../spec/srs.md), 13 термінів: Власник, Пацієнт, Медична картка, Архівна картка, Прийом, Запис на прийом, Слот розкладу, Конфлікт розкладу, Каталог медикаментів, Медикамент, Залишок препарату, Адміністратор, Лікар.
- **Затверджений мапінг «глосарій ↔ API»:** [`spec/api/openapi.yaml`, розділ «Відповідність термінам предметної області»](../spec/api/openapi.yaml). Цей мапінг є частиною контракту, тож відхилення, зафіксовані в ньому, вважаються свідомими рішеннями.
- **Додаткові терміни з адендумів:** сповіщення про низький залишок (REQ-F-047), неробочий день (addendum-004), журнал аудиту (addendum-003).

## 2. Що проаналізовано

У `src/main` перевірено 111 Java-файлів: сутності `*Entity`, DTO `*Request`/`*Response`/`*Page`, репозиторії, сервіси, контролери, enum `Role`, `AppointmentStatus`, `SlotStatus`, `PatientStatus`, публічні методи, REST-шляхи, таблиці й колонки Flyway-міграцій. Окремо код перевірено на синоніми, яких немає в глосарії (pet, animal, client, drug, booking, occupied, remaining тощо).

Імена контролерів і їхніх методів збігаються з `operationId` в OpenAPI (`createAppointment`, `rescheduleAppointment`, `getDoctorSchedule`, `adjustMedicationStock`, …). Ресурси `owners`, `patients`, `appointments`, `visits`, `medications`, `notifications` і статуси `SCHEDULED`/`COMPLETED`/`CANCELLED`, `ACTIVE`/`ARCHIVED`, `FREE`/`BOOKED` відповідають глосарію.

## 3. Таблиця розбіжностей

| # | Термін глосарію | Поточна назва в коді | Ризик | Рішення |
|---|---|---|---|---|
| 1 | Слот… «зайнятий» (`BOOKED` в API) | `ScheduleService`: приватний метод `occupiedSlots()`, локальні змінні й параметр `occupied` | Низький | **Виправлено** → `bookedSlots()`, `booked` |
| 2 | Залишок препарату (Medication Stock) | `WithdrawnMedication.remaining` (компонент внутрішнього record) | Низький | **Виправлено** → `remainingStock`. Тепер не плутається з `quantity`, що в цьому record означає списану кількість |
| 3 | Списання медикаменту під час прийому | `medications/StockRequest` — внутрішній record, не HTTP-DTO | Низький | **Виправлено** → `StockWithdrawal`. Суфікс `*Request` у проєкті позначає тіло HTTP-запиту, і стара назва плутала його з DTO `StockAdjustmentRequest` |
| 4 | Лікар (Veterinarian) | REST `/api/v1/doctors`, `/doctors/{doctorId}/schedule`; JSON-поля `doctorId`, `doctor`; колонки `doctor_id`; класи `DoctorController`, `Doctors`, `DoctorInfo` | Високий | **Мапінг, без змін.** OpenAPI прямо фіксує скорочення `doctor` у шляхах і полях. Роль лишається `VETERINARIAN` (`Role`, `UserEntity.isVeterinarian()`). Java-класи свідомо використовують ту саму лексику, що й API |
| 5 | Залишок препарату (Medication Stock) | JSON-поле і колонка `quantity` медикамента; операція `adjust-stock` | Високий | **Мапінг, без змін.** OpenAPI: «поле `quantity` медикамента, операція `adjust-stock`» |
| 6 | Адміністратор (Administrator) | `Role.ADMIN`, значення в JWT і колонці `users.role` | Високий | **Мапінг, без змін.** OpenAPI: «роль `ADMIN`» |
| 7 | Конфлікт розкладу (Schedule Conflict) | код помилки `SLOT_TAKEN`, приватний `slotTaken()` | Високий для коду помилки | **Мапінг, без змін.** OpenAPI: «відповідь 409 з кодом `SLOT_TAKEN`». Приватний метод узгоджений із кодом помилки, тому теж не перейменовувався |
| 8 | Медична картка / Архівна картка (Medical / Archived Record) | окремої сутності немає: хронологія `GET /patients/{patientId}/visits`, статус `PatientStatus.ARCHIVED`, `PatientArchiveGuard` | Високий (новий ресурс змінив би API) | **Мапінг, без змін.** OpenAPI: «не окремий ресурс» |
| 9 | Слот «зайнятий» | Flyway-індекс `appointments_occupied_slot_uq` (V5) | Високий | **Без змін.** Перейменування потребує нової міграції, а вже застосовану міграцію V5 змінювати не можна (Flyway перевіряє контрольну суму). У коді на назву індексу ніхто не посилається; розбіжність можна закрити окремою міграцією `ALTER INDEX … RENAME`, якщо команда вирішить |
| 10 | Пацієнт (Patient) | — | — | Розбіжностей немає: `Pet`/`Animal` у коді не трапляються, скрізь `Patient` |

## 4. Виправлено

| Файл | Було | Стало |
|---|---|---|
| `appointments/ScheduleService.java` | `occupiedSlots(...)`, `occupied` | `bookedSlots(...)`, `booked` |
| `medications/WithdrawnMedication.java`, `visits/VisitService.java` | `remaining`, `remaining()` | `remainingStock`, `remainingStock()` |
| `medications/StockRequest.java` → `medications/StockWithdrawal.java`; `MedicationStock.java`, `VisitService.java` | `StockRequest` | `StockWithdrawal` |

Ці зміни не зачіпають URL-шляхи, JSON-поля, таблиці й колонки БД. Перейменовані типи й методи використовуються тільки всередині процесу.

## 5. Без змін (з обґрунтуванням)

Пункти 4–9 таблиці. Пункти 4–8 описано в затвердженому мапінгу OpenAPI. Зміна будь-якого з них зламала б контракт API, від якого залежать тести й клієнти, і суперечила б специфікації. Пункт 9 — назва об'єкта БД у вже застосованій міграції.

## 6. Перевірка

| Перевірка | Результат |
|---|---|
| `./mvnw test` | `Tests run: 745, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS` |
| `./mvnw checkstyle:check` | 110 порушень, усі — відсутній Javadoc (`MissingJavadocMethod` 70, `MissingJavadocType` 40), як і до аудиту. Нових порушень стилю немає |
| `./mvnw spotless:apply` | код відформатовано google-java-format після перейменувань |
