# API-довідник vet-clinic-api

Довідник згенеровано автоматично 2026-11-24 командою [`./docs/api/generate.sh`](generate.sh). Вручну HTML не редагується: щоб оновити довідник, змініть джерела й перезапустіть скрипт.

| Частина | Відкрити | Джерело | Інструмент |
|---|---|---|---|
| Довідник REST API (32 операції, схеми, коди помилок, приклади) | [`rest/index.html`](rest/index.html), один самодостатній HTML-файл | [`spec/api/openapi.yaml`](../../spec/api/openapi.yaml), OpenAPI 3.0.3 | Redocly CLI 1.34.5 (`redocly build-docs`, Redoc), Docker-образ `redocly/cli:1.34.5` |
| Довідник Java-коду (12 пакетів) | [`javadoc/index.html`](javadoc/index.html) | Javadoc у `src/main/java` | `maven-javadoc-plugin` (`./mvnw javadoc:javadoc`, doclint `all,-missing`, `failOnWarnings`) |

Інтерактивну версію REST-довідника з «Try it out» дає Swagger UI з `docker compose` (див. [README](../../README.md#swagger-ui)).

## Як згенеровано

Скрипт [`generate.sh`](generate.sh) виконує три кроки й зупиняється на першій помилці:

1. `redocly lint spec/api/openapi.yaml`: специфікація валідна, 0 помилок і 1 попередження `info-license`. У контракті немає поля `license`, і його значення має задати людина.
2. `redocly build-docs` створює `rest/index.html`.
3. `./mvnw javadoc:javadoc` збирає Javadoc без помилок і попереджень doclint, після чого `target/reports/apidocs` копіюється в `javadoc/`.

Довідник відповідає коду, бо контракт перевіряє тест `OpenApiContractConsistencyTest`. Він звіряє всі ендпоінти Spring MVC з `openapi.yaml` (метод, шлях, `operationId`) і генерує таблицю нижче. Запуск 2026-11-24 пройшов успішно: 10 контролерів, 32 ендпоінти, розбіжностей немає.

## Ендпоінти

Базовий шлях `/api/v1`. Посилання ведуть на операцію в REST-довіднику та на клас контролера в Javadoc. Ролі взято з розширення `x-roles`.

| Метод | Шлях | operationId | Ролі | Контролер | Резюме |
|---|---|---|---|---|---|
| GET | `/appointments` | [listAppointments](rest/index.html#operation/listAppointments) | ADMIN, VETERINARIAN | [AppointmentController](javadoc/ua/vetclinic/api/appointments/AppointmentController.html) | Список записів на прийом |
| POST | `/appointments` | [createAppointment](rest/index.html#operation/createAppointment) | ADMIN | [AppointmentController](javadoc/ua/vetclinic/api/appointments/AppointmentController.html) | Створити запис на прийом |
| GET | `/appointments/{appointmentId}` | [getAppointment](rest/index.html#operation/getAppointment) | ADMIN, VETERINARIAN | [AppointmentController](javadoc/ua/vetclinic/api/appointments/AppointmentController.html) | Отримати запис на прийом |
| PATCH | `/appointments/{appointmentId}` | [rescheduleAppointment](rest/index.html#operation/rescheduleAppointment) | ADMIN | [AppointmentController](javadoc/ua/vetclinic/api/appointments/AppointmentController.html) | Перенести запис на інший слот |
| POST | `/appointments/{appointmentId}/cancel` | [cancelAppointment](rest/index.html#operation/cancelAppointment) | ADMIN | [AppointmentController](javadoc/ua/vetclinic/api/appointments/AppointmentController.html) | Скасувати запис на прийом |
| POST | `/appointments/{appointmentId}/visit` | [createVisit](rest/index.html#operation/createVisit) | VETERINARIAN | [VisitController](javadoc/ua/vetclinic/api/visits/VisitController.html) | Зберегти результати прийому |
| POST | `/auth/login` | [login](rest/index.html#operation/login) | без автентифікації | [AuthController](javadoc/ua/vetclinic/api/auth/AuthController.html) | Вхід у систему |
| GET | `/doctors` | [listDoctors](rest/index.html#operation/listDoctors) | ADMIN, VETERINARIAN | [DoctorController](javadoc/ua/vetclinic/api/users/DoctorController.html) | Довідник активних лікарів |
| GET | `/doctors/{doctorId}/schedule` | [getDoctorSchedule](rest/index.html#operation/getDoctorSchedule) | ADMIN, VETERINARIAN | [ScheduleController](javadoc/ua/vetclinic/api/appointments/ScheduleController.html) | Сітка розкладу лікаря |
| GET | `/medications` | [listMedications](rest/index.html#operation/listMedications) | ADMIN, VETERINARIAN | [MedicationController](javadoc/ua/vetclinic/api/medications/MedicationController.html) | Каталог медикаментів |
| POST | `/medications` | [createMedication](rest/index.html#operation/createMedication) | ADMIN | [MedicationController](javadoc/ua/vetclinic/api/medications/MedicationController.html) | Додати медикамент до каталогу |
| GET | `/medications/{medicationId}` | [getMedication](rest/index.html#operation/getMedication) | ADMIN, VETERINARIAN | [MedicationController](javadoc/ua/vetclinic/api/medications/MedicationController.html) | Отримати медикамент |
| PATCH | `/medications/{medicationId}` | [updateMedication](rest/index.html#operation/updateMedication) | ADMIN | [MedicationController](javadoc/ua/vetclinic/api/medications/MedicationController.html) | Редагувати медикамент (назва, мінімальний поріг) |
| POST | `/medications/{medicationId}/adjust-stock` | [adjustMedicationStock](rest/index.html#operation/adjustMedicationStock) | ADMIN | [MedicationController](javadoc/ua/vetclinic/api/medications/MedicationController.html) | Ручне коригування залишку |
| GET | `/notifications` | [listNotifications](rest/index.html#operation/listNotifications) | ADMIN | [NotificationController](javadoc/ua/vetclinic/api/notifications/NotificationController.html) | Сповіщення про низький залишок |
| POST | `/notifications/{notificationId}/mark-read` | [markNotificationRead](rest/index.html#operation/markNotificationRead) | ADMIN | [NotificationController](javadoc/ua/vetclinic/api/notifications/NotificationController.html) | Позначити сповіщення прочитаним |
| GET | `/owners` | [listOwners](rest/index.html#operation/listOwners) | ADMIN, VETERINARIAN | [OwnerController](javadoc/ua/vetclinic/api/owners/OwnerController.html) | Пошук і список власників |
| POST | `/owners` | [createOwner](rest/index.html#operation/createOwner) | ADMIN | [OwnerController](javadoc/ua/vetclinic/api/owners/OwnerController.html) | Зареєструвати власника |
| GET | `/owners/{ownerId}` | [getOwner](rest/index.html#operation/getOwner) | ADMIN, VETERINARIAN | [OwnerController](javadoc/ua/vetclinic/api/owners/OwnerController.html) | Отримати власника |
| PATCH | `/owners/{ownerId}` | [updateOwner](rest/index.html#operation/updateOwner) | ADMIN | [OwnerController](javadoc/ua/vetclinic/api/owners/OwnerController.html) | Редагувати власника |
| GET | `/patients` | [listPatients](rest/index.html#operation/listPatients) | ADMIN, VETERINARIAN | [PatientController](javadoc/ua/vetclinic/api/patients/PatientController.html) | Пошук і список пацієнтів |
| POST | `/patients` | [createPatient](rest/index.html#operation/createPatient) | ADMIN | [PatientController](javadoc/ua/vetclinic/api/patients/PatientController.html) | Зареєструвати пацієнта |
| GET | `/patients/{patientId}` | [getPatient](rest/index.html#operation/getPatient) | ADMIN, VETERINARIAN | [PatientController](javadoc/ua/vetclinic/api/patients/PatientController.html) | Отримати пацієнта |
| PATCH | `/patients/{patientId}` | [updatePatient](rest/index.html#operation/updatePatient) | ADMIN | [PatientController](javadoc/ua/vetclinic/api/patients/PatientController.html) | Редагувати базові дані пацієнта |
| POST | `/patients/{patientId}/archive` | [archivePatient](rest/index.html#operation/archivePatient) | ADMIN | [PatientController](javadoc/ua/vetclinic/api/patients/PatientController.html) | Перевести картку пацієнта в статус «Архівна» |
| GET | `/patients/{patientId}/visits` | [listPatientVisits](rest/index.html#operation/listPatientVisits) | VETERINARIAN | [VisitController](javadoc/ua/vetclinic/api/visits/VisitController.html) | Хронологія прийомів пацієнта (медична картка) |
| GET | `/users` | [listUsers](rest/index.html#operation/listUsers) | ADMIN | [UserController](javadoc/ua/vetclinic/api/users/UserController.html) | Список облікових записів |
| POST | `/users` | [createUser](rest/index.html#operation/createUser) | ADMIN | [UserController](javadoc/ua/vetclinic/api/users/UserController.html) | Створити обліковий запис |
| GET | `/users/{userId}` | [getUser](rest/index.html#operation/getUser) | ADMIN | [UserController](javadoc/ua/vetclinic/api/users/UserController.html) | Отримати обліковий запис |
| POST | `/users/{userId}/deactivate` | [deactivateUser](rest/index.html#operation/deactivateUser) | ADMIN | [UserController](javadoc/ua/vetclinic/api/users/UserController.html) | Деактивувати обліковий запис |
| GET | `/visits/{visitId}` | [getVisit](rest/index.html#operation/getVisit) | VETERINARIAN | [VisitController](javadoc/ua/vetclinic/api/visits/VisitController.html) | Отримати прийом |
| PATCH | `/visits/{visitId}` | [updateVisit](rest/index.html#operation/updateVisit) | VETERINARIAN | [VisitController](javadoc/ua/vetclinic/api/visits/VisitController.html) | Редагувати результати прийому |

`DELETE` на будь-який ресурс повертає 405 із заголовком `Allow` (REQ-F-016). Модель помилок `Error`, пагінацію, порядок перевірок 401 → 403 → 422 → 404 → 409 і відповідність термінам глосарію описано у вступі REST-довідника.
