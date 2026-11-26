# Addendum 005 до SRS v1.0: критерії прийняття для вимог без формальних критеріїв

| Поле | Значення |
|---|---|
| Тікет | TSK-018 (Validation: «критерії прийняття §9.1»), TSK-017 (Documentation: матриця простежуваності) |
| Дата | 2026-11-26 |
| Статус | **Approved** (затверджено людиною KarasovDmytro, 2026-11-26; без змін) |
| Підстава | Аудит матриці простежуваності (`logs/traceability-audit-2026-11-26.md`, розділ 2.1): SRS §9.1 і Addendum 004 задають критерії прийняття лише для 6 з 43 вимог (REQ-F-004, 016, 023, 035, 046, 049). Для решти 37 колонка «Критерій прийняття» порожня |
| Нормативний контракт | `spec/api/openapi.yaml`. Критерії не змінюють контракт; у разі розбіжності діє OpenAPI |

## Характер документа

Критерії **ретроспективні**: сформульовані після реалізації. Вони **не вводять нової поведінки** і не змінюють жодної вимоги. Кожен критерій перефразовує в перевірюваній формі те, що вже затверджено:

- текст вимоги в SRS або адендумі;
- рішення адендумів 001–004 (AD-01…AD-25);
- опис операції в `spec/api/openapi.yaml`.

Для кожного критерію вказано, які тести його перевіряють (`Клас#метод` у `src/test/java/ua/vetclinic/api/`). Якщо тесту немає, це сказано прямо. Критерії не підганялися під тести: де тест перевіряє більше або менше, ніж критерій, перевага за вимогою.

**Ідентифікатори.** `AC-<вимога>.<n>`, наприклад `AC-F-001.1` — перший критерій REQ-F-001. Нумерація незалежна від §9.1 SRS.

**Формат.** «Умова → дія → очікуваний результат». Коди помилок — за моделлю `Error` (AD-18).

---

## 7.1 Управління користувачами

### REQ-F-001 — Створення облікового запису адміністратором (AD-01, AD-11, AD-17)

- **AC-F-001.1.** Адміністратор створює обліковий запис із роллю `VETERINARIAN` або `ADMIN`, валідним email, ПІБ і паролем 8–72 символи → 201, `active: true`, у відповіді немає пароля; запис доступний через `GET /users/{id}`.
  Перевіряє: `CreateUserTest#adminCreatesVeterinarianAccount_returns201WithoutPassword`, `GetUserTest#adminGetsCreatedUserByLocation_returns200WithoutPassword`.
- **AC-F-001.2.** Email уже використано, зокрема в іншому регістрі → 409 `EMAIL_TAKEN`, другий обліковий запис не створюється.
  Перевіряє: `CreateUserTest#adminCreatesUserWithTakenEmail_returns409EmailTaken`; незалежність від регістру — на рівні бази даних: `UserEmailUniquenessTest#emailInOtherCase_isRejectedByDatabase`. Відповідь API 409 для email в іншому регістрі окремим тестом не перевіряється; її підтверджено лише вручну під час прогону сценаріїв посібника користувача 2026-11-24.
- **AC-F-001.3.** Відсутнє обов'язкове поле або значення поза межами (пароль < 8 чи > 72 символів, email > 254, ПІБ > 150, невідома роль) → 422 `VALIDATION_ERROR` з полем і причиною.
  Перевіряє: `CreateUserTest#adminCreatesUserWithTooShortPassword_returnsTooShortReason`, `CreateUserTest#adminCreatesUserWithTooLongEmail_returnsTooLongReason`, `CreateUserTest#adminCreatesUserWithUnknownRole_returns422InvalidEnum`.
- **AC-F-001.4.** Перший адміністратор створюється під час розгортання з налаштувань і може увійти з роллю `ADMIN` (AD-01.5).
  Перевіряє: `InitialAdminSeedTest#configuredInitialAdmin_canLogIn`.

### REQ-F-002 — Деактивація облікового запису (AD-01, AD-16, AD-20)

- **AC-F-002.1.** Адміністратор деактивує обліковий запис → 200, `active: false`.
  Перевіряє: `DeactivateUserTest#adminDeactivatesVeterinarian_returns200WithActiveFalse`.
- **AC-F-002.2.** Після деактивації вже виданий токен цього користувача одразу перестає діяти: будь-який запит з ним → 401 (AD-01).
  Перевіряє: `BearerAuthenticationTest#tokenOfDeactivatedUser_returns401Unauthorized`.
- **AC-F-002.3.** Повторна деактивація → 409 `USER_ALREADY_DEACTIVATED`; деактивація останнього активного адміністратора → 409 `LAST_ADMIN`; дані не змінюються.
  Перевіряє: `DeactivateUserTest#adminDeactivatesAlreadyDeactivatedUser_returns409UserAlreadyDeactivated`, `LastAdminTest#deactivatingLastActiveAdmin_returns409LastAdmin`.

### REQ-F-003 — Деактивований користувач не може увійти (AD-01)

- **AC-F-003.1.** Деактивований користувач вводить правильні email і пароль → 403 `ACCOUNT_DEACTIVATED`, токен не видається.
  Перевіряє: `LoginTest#deactivatedUser_returns403AccountDeactivated`.
- **AC-F-003.2.** Активний користувач із правильними даними → 200 і bearer-токен на 8 годин; невірний пароль або невідомий email → однакова відповідь 401 `INVALID_CREDENTIALS`.
  Перевіряє: `LoginTest#validCredentials_returns200WithBearerTokenAndUser`, `LoginTest#wrongPassword_returns401InvalidCredentials`, `LoginTest#unknownEmail_returns401InvalidCredentialsWithSameMessage`.

### REQ-F-005 — Лікар без доступу до облікових записів (AD-06)

- **AC-F-005.1.** Лікар викликає створення, перегляд, список чи деактивацію облікових записів → 403 `FORBIDDEN`, дані не змінюються; перевірка ролі виконується раніше за валідацію тіла.
  Перевіряє: `UserAccessControlTest#veterinarianCreatesUser_returns403`, `UserAccessControlTest#veterinarianCreatesUserWithInvalidBody_returns403BeforeValidation`, `GetUserTest#veterinarianGetsUser_returns403`.

## 7.2 Власники та пацієнти

### REQ-F-010 — Реєстрація власника (AD-10, AD-17)

- **AC-F-010.1.** Адміністратор реєструє власника з ПІБ і телефоном (email і адреса необов'язкові) → 201, `Location`, дані повертаються через `GET /owners/{id}`.
  Перевіряє: `CreateOwnerTest#adminCreatesOwner_returns201WithOwner`, `CreateOwnerTest#adminCreatesOwnerWithoutOptionalFields_returns201WithNulls`, `GetOwnerTest#adminGetsOwner_returns200WithOwner`.
- **AC-F-010.2.** Немає ПІБ або телефону, телефон не за шаблоном `^\+?[0-9]{10,15}$`, email не за форматом → 422 з полем і причиною; власник не створюється.
  Перевіряє: `CreateOwnerTest#missingFullName_returns422Required`, `CreateOwnerTest#phoneWithSpaces_returns422InvalidFormat`, `CreateOwnerValidationTest#invalidEmail_returns422InvalidFormat`.

### REQ-F-011 — Редагування власника (AD-10, AD-17)

- **AC-F-011.1.** Адміністратор змінює одне або кілька полів → 200; передані поля оновлено, відсутні лишилися без змін, `null` очищає необов'язкове поле.
  Перевіряє: `UpdateOwnerTest#adminUpdatesFullName_returns200AndKeepsOtherFields`, `UpdateOwnerValidationTest#absentFields_remainUnchanged`, `UpdateOwnerTest#nullEmail_clearsOptionalField`.
- **AC-F-011.2.** Невалідне значення або порожнє тіло → 422 (`EMPTY_BODY` для `{}`), дані власника не змінюються.
  Перевіряє: `UpdateOwnerTest#emptyBody_returns422EmptyBody`, `UpdateOwnerValidationTest#rejectedUpdate_leavesOwnerUnchanged`.

### REQ-F-012 — Реєстрація пацієнта з прив'язкою до власника (AD-10, AD-17)

- **AC-F-012.1.** Адміністратор реєструє пацієнта з `ownerId` існуючого власника, кличкою й видом → 201, статус `ACTIVE` від сервера, стать за замовчуванням `UNKNOWN`.
  Перевіряє: `CreatePatientTest#adminCreatesPatient_returns201WithActiveStatus`, `CreatePatientTest#minimalFields_applyDefaults`.
- **AC-F-012.2.** `ownerId` не вказано → 422 `REQUIRED`; власника не існує → 422 `REFERENCE_NOT_FOUND`; пацієнт не створюється.
  Перевіряє: `CreatePatientValidationTest#missingOwnerId_returns422Required`, `CreatePatientTest#unknownOwner_returns422ReferenceNotFound`.

### REQ-F-013 — Редагування базових даних пацієнта (AD-10, AD-17)

- **AC-F-013.1.** Адміністратор змінює кличку, вид, породу, стать або дату народження → 200; власник і статус лишаються тими самими.
  Перевіряє: `UpdatePatientTest#updateName_returns200AndKeepsOwnerAndStatus`, `UpdatePatientTest#updateAllFields_returns200AndPersists`.
- **AC-F-013.2.** Невалідне значення (дата народження в майбутньому, кличка > 100 символів) → 422, дані пацієнта не змінюються.
  Перевіряє: `UpdatePatientTest#futureBirthDate_returns422OutOfRange`, `UpdatePatientTest#rejectedUpdate_leavesPatientUnchanged`.

### REQ-F-014 — Рівно один власник, зміна власника не передбачена (AD-10, AD-19)

- **AC-F-014.1.** У одного власника може бути кілька пацієнтів; кожен пацієнт має рівно одне значення `ownerId`.
  Перевіряє: `CreatePatientTest#ownerCanHaveSeveralPatients`.
- **AC-F-014.2.** Запит на редагування з полем `ownerId` (навіть з тим самим значенням) → 422 `IMMUTABLE_FIELD`, власник не змінюється.
  Перевіряє: `UpdatePatientTest#ownerIdInBody_returns422ImmutableField`, `UpdatePatientTest#sameOwnerIdInBody_returns422ImmutableField`.

### REQ-F-015 — Переведення картки в статус «Архівна» (AD-04, AD-16)

- **AC-F-015.1.** Адміністратор архівує картку пацієнта без записів зі статусом «Заплановано» → 200, `status: ARCHIVED`, решта даних без змін; інші пацієнти й власник не змінюються.
  Перевіряє: `ArchivePatientTest#adminArchivesPatient_returns200WithArchivedStatus`, `ArchivePatientTest#archive_doesNotAffectOtherPatientsOrOwner`.
- **AC-F-015.2.** Пацієнт має хоча б один запис зі статусом «Заплановано» → 409 `PATIENT_HAS_SCHEDULED_APPOINTMENTS`, картка лишається `ACTIVE`; після скасування записів архівація проходить.
  Перевіряє: `PatientArchiveWithAppointmentsTest#patientWithScheduledAppointment_cannotBeArchived`, `PatientArchiveWithAppointmentsTest#afterCancellingAppointment_patientCanBeArchived`.
- **AC-F-015.3.** Архівація необоротна: повторна → 409 `PATIENT_ARCHIVED`, статус `ACTIVE` не повертається.
  Перевіряє: `ArchivePatientTest#archiveTwice_returns409PatientArchived`, `ArchivePatientTest#archive_isIrreversible`.

### REQ-F-017 — Пошук власників і пацієнтів за іменем (AD-15)

- **AC-F-017.1.** Пошук із параметром `q` (2–100 символів) повертає власників, у ПІБ яких є підрядок, і пацієнтів, у кличці яких є підрядок, без урахування регістру; символи `%` і `_` шукаються буквально.
  Перевіряє: `ListOwnersTest#searchByQuery_returnsOwnersWithSubstringIgnoringCase`, `ListPatientsTest#searchByQuery_returnsPatientsWithSubstringIgnoringCase`, `ListOwnersPagingTest#wildcardCharactersInQuery_areLiteral`.
- **AC-F-017.2.** Пошук без збігів → 200, `total: 0`, порожній `items`; `q` з одного символу → 422 `TOO_SHORT`.
  Перевіряє: `ListOwnersPagingTest#queryWithoutMatches_returnsEmptyPage`, `ListOwnersTest#searchWithOneCharacter_returns422TooShort`.

## 7.3 Розклад

### REQ-F-020 — Сітка розкладу зі слотами по 30 хвилин (AD-05, AD-17, AD-20, AD-25)

- **AC-F-020.1.** Розклад лікаря на робочий день без записів → 18 слотів `FREE` по 30 хвилин з 09:00 до 18:00 за часом клініки.
  Перевіряє: `DoctorScheduleTest#emptyDay_hasEighteenFreeSlotsFrom0900To1800`, `DoctorScheduleTest#everySlot_hasOffsetTimestampsAndThirtyMinuteDuration`.
- **AC-F-020.2.** Слот із записом «Заплановано» або «Завершено» → `BOOKED` з даними запису; скасований запис слот звільняє.
  Перевіряє: `DoctorScheduleTest#scheduledAppointment_marksSlotBooked`, `CreateVisitTest#completedAppointment_keepsSlotOccupied`, `DoctorScheduleTest#cancelledAppointment_freesSlot`.
- **AC-F-020.3.** Діапазон до 7 днів включно → 200; довший → 422 `RANGE_TOO_LARGE`; `from` пізніше за `to` → 422 `INVALID_RANGE`.
  Перевіряє: `DoctorScheduleTest#sevenDayRange_returns90SortedSlots`, `DoctorScheduleTest#eightDayRange_returns422RangeTooLarge`, `DoctorScheduleTest#fromAfterTo_returns422InvalidRange`.

### REQ-F-021 — Створення запису на прийом (AD-04, AD-05, AD-17, AD-25)

- **AC-F-021.1.** Адміністратор створює запис для активного лікаря, неархівного пацієнта й вільного слота в робочий час → 201, `endsAt` = `startsAt` + 30 хвилин, запис повертається через `GET`.
  Перевіряє: `CreateAppointmentTest#adminCreatesAppointment_returns201WithScheduledStatus`, `CreateAppointmentTest#createdAppointment_isReturnedByLocation`.
- **AC-F-021.2.** Час не на межі слота або поза робочими годинами → 422 (`SLOT_NOT_ALIGNED` / `OUTSIDE_WORKING_HOURS`); лікар деактивований → 422 `DOCTOR_NOT_AVAILABLE`; пацієнт архівний → 409 `PATIENT_ARCHIVED`; запис не створюється.
  Перевіряє: `CreateAppointmentValidationTest#minutesNotOnSlotBoundary_returns422SlotNotAligned`, `CreateAppointmentValidationTest#atEndOfWorkingDay_returns422OutsideWorkingHours`, `CreateAppointmentTest#deactivatedDoctor_returns422DoctorNotAvailable`, `CreateAppointmentTest#archivedPatient_returns409PatientArchived`.

### REQ-F-022 — Статус «Заплановано» при створенні (AD-19)

- **AC-F-022.1.** Новий запис отримує статус `SCHEDULED`, який задає сервер.
  Перевіряє: `CreateAppointmentTest#adminCreatesAppointment_returns201WithScheduledStatus`.
- **AC-F-022.2.** Спроба передати `status` у запиті створення → 422 `UNKNOWN_FIELD`.
  Перевіряє: `CreateAppointmentTest#statusInBody_returns422UnknownField`.

### REQ-F-024 — Перенесення запису на інший вільний слот (AD-03, AD-20, AD-25)

- **AC-F-024.1.** Адміністратор переносить запис «Заплановано» на вільний слот → 200; `id`, лікар, пацієнт і статус ті самі; старий слот одразу вільний, новий зайнятий.
  Перевіряє: `RescheduleAppointmentTest#rescheduleToFreeSlot_returns200AndKeepsIdentity`, `RescheduleAppointmentTest#reschedule_freesOldSlotAndBooksNewOne`.
- **AC-F-024.2.** Новий слот зайнятий → 409 `SLOT_TAKEN`; запис скасовано → 409 `APPOINTMENT_NOT_SCHEDULED`; лікаря деактивовано → 422 `DOCTOR_NOT_AVAILABLE`; в усіх випадках запис лишається на старому слоті.
  Перевіряє: `RescheduleAppointmentTest#rescheduleToTakenSlot_returns409AndKeepsOldSlot`, `RescheduleAppointmentTest#rescheduleCancelledAppointment_returns409`, `RescheduleAppointmentTest#rescheduleAppointmentOfDeactivatedDoctor_returns422`.

### REQ-F-025 — Скасування запису (AD-03, AD-16, AD-20)

- **AC-F-025.1.** Адміністратор скасовує запис «Заплановано» → 200, статус `CANCELLED`; запис не видаляється й повертається через `GET`; слот одразу вільний.
  Перевіряє: `CancelAppointmentTest#adminCancelsAppointment_returns200WithCancelledStatus`, `CancelAppointmentTest#cancelledAppointment_isStillReturnedByGet`, `CancelAppointmentTest#cancel_freesSlotInSchedule`.
- **AC-F-025.2.** Повторне скасування або скасування завершеного запису → 409 `APPOINTMENT_NOT_SCHEDULED`.
  Перевіряє: `CancelAppointmentTest#cancelTwice_returns409AppointmentNotScheduled`, `CreateVisitTest#completedAppointment_cannotBeCancelled`.

### REQ-F-026 — Лікар лише переглядає розклад (AD-06)

- **AC-F-026.1.** Лікар отримує розклад будь-якого лікаря, список і окремий запис → 200.
  Перевіряє: `DoctorScheduleTest#veterinarianViewsAnyDoctorSchedule_returns200`, `ListAppointmentsTest#veterinarianListsAppointments_returns200`, `GetAppointmentTest#veterinarianGetsAppointment_returns200`.
- **AC-F-026.2.** Лікар створює, переносить або скасовує запис → 403 `FORBIDDEN`, запис не змінюється.
  Перевіряє: `CreateAppointmentTest#veterinarianCreatesAppointment_returns403`, `RescheduleAppointmentTest#veterinarianReschedules_returns403`, `CancelAppointmentTest#veterinarianCancels_returns403`.

### REQ-F-027 — Статус «Завершено» при збереженні прийому (AD-07, AD-19)

- **AC-F-027.1.** Після збереження прийому запис отримує статус `COMPLETED` і посилання `visitId`; слот лишається зайнятим.
  Перевіряє: `CreateVisitTest#createVisit_completesAppointment`, `CreateVisitTest#completedAppointment_keepsSlotOccupied`.
- **AC-F-027.2.** Якщо прийом не збережено (наприклад, через нестачу медикаменту), запис лишається `SCHEDULED`.
  Перевіряє: `CreateVisitStockTest#insufficientStock_changesNothing`.

## 7.4 Медична картка

### REQ-F-030 — Доступ лікаря до картки будь-якого пацієнта (AD-06)

- **AC-F-030.1.** Лікар, який не працював із пацієнтом, отримує хронологію його прийомів і окремий прийом іншого лікаря → 200.
  Перевіряє: `ListPatientVisitsTest#otherDoctor_seesHistory`, `GetVisitTest#otherDoctorGetsVisit_returns200`.

### REQ-F-031 — Адміністратор без доступу до медичних даних (AD-18)

- **AC-F-031.1.** Адміністратор запитує хронологію прийомів, окремий прийом, збереження чи редагування прийому → 403 `FORBIDDEN`, зокрема для архівної картки й неіснуючого прийому (наявність не розкривається).
  Перевіряє: `ListPatientVisitsTest#admin_returns403`, `ListPatientVisitsTest#adminOnArchivedCard_returns403`, `GetVisitTest#adminOnUnknownVisit_returns403Too`, `CreateVisitTest#admin_returns403`, `UpdateVisitTest#admin_returns403`.
- **AC-F-031.2.** Базові дані пацієнта (без діагнозів і призначень) адміністратору доступні → 200.
  Перевіряє: `ListPatientVisitsTest#adminStillReadsPatient`.

### REQ-F-032 — Фіксація діагнозу, призначень і нотаток (AD-07, AD-12)

- **AC-F-032.1.** Лікар, якому призначено запис «Заплановано», зберігає прийом з діагнозом (призначення й нотатки необов'язкові) → 201 з результатами.
  Перевіряє: `CreateVisitTest#assignedDoctorCreatesVisit_returns201`, `CreateVisitTest#onlyDiagnosis_isEnough`.
- **AC-F-032.2.** Немає діагнозу → 422 `REQUIRED`, запис лишається `SCHEDULED`; інший лікар → 403 `NOT_ASSIGNED_DOCTOR`.
  Перевіряє: `CreateVisitValidationTest#missingDiagnosis_returns422Required`, `CreateVisitTest#otherDoctor_returns403`.
- **AC-F-032.3.** Автор змінює діагноз, призначення чи нотатки → 200, змінено лише передані поля; інший лікар → 403 `NOT_VISIT_AUTHOR`.
  Перевіряє: `UpdateVisitTest#authorChangesDiagnosis_changesOnlyDiagnosis`, `UpdateVisitTest#otherDoctor_returns403`.

### REQ-F-033 — Дата, лікар і пацієнт у кожному прийомі (AD-14)

- **AC-F-033.1.** Кожен прийом містить `visitDate`, `doctorId` і `patientId`; `visitDate` дорівнює початку запису, а не моменту збереження.
  Перевіряє: `GetVisitTest#authorGetsVisit_returnsAllFields`, `CreateVisitTest#visitDate_equalsAppointmentStart`.
- **AC-F-033.2.** `visitDate` і `doctorId` не можна задати чи змінити запитом → 422 `UNKNOWN_FIELD`.
  Перевіряє: `CreateVisitValidationTest#visitDateInRequest_returns422UnknownField`, `CreateVisitValidationTest#doctorIdInRequest_returns422UnknownField`, `UpdateVisitTest#visitDateInRequest_returns422`.

### REQ-F-034 — Повна хронологія прийомів (AD-14)

- **AC-F-034.1.** Хронологія пацієнта містить усі його прийоми всіх лікарів і лише їх, відсортовані за `visitDate` від найновішого.
  Перевіряє: `ListPatientVisitsTest#visits_areSortedByVisitDateDescending`, `ListPatientVisitsTest#history_includesVisitsOfAllDoctors`, `ListPatientVisitsTest#history_containsOnlyThisPatientsVisits`.
- **AC-F-034.2.** Пацієнт без прийомів → 200, порожній список, `total: 0`.
  Перевіряє: `ListPatientVisitsTest#patientWithoutVisits_returnsEmptyPage`.

## 7.5 Облік медикаментів

### REQ-F-040 — Додавання медикаменту з назвою й залишком (AD-13, AD-17)

- **AC-F-040.1.** Адміністратор додає медикамент з назвою й початковим залишком (ціле від 0) → 201, `Location`.
  Перевіряє: `CreateMedicationTest#adminCreatesMedication_returns201`.
- **AC-F-040.2.** Назва вже є в каталозі без урахування регістру й пробілів по краях → 409 `MEDICATION_NAME_TAKEN`.
  Перевіряє: `CreateMedicationTest#duplicateName_returns409`, `CreateMedicationTest#duplicateNameWithOtherCase_returns409`, `CreateMedicationTest#duplicateNameWithSpaces_returns409`.
- **AC-F-040.3.** Немає назви чи залишку, назва > 150 символів, залишок поза 0–1 000 000 → 422; дробовий залишок → 400.
  Перевіряє: `CreateMedicationValidationTest#emptyObject_listsBothRequiredFields`, `CreateMedicationValidationTest#tooLongName_returns422TooLong`, `CreateMedicationValidationTest#fractionalQuantity_returns400`.

### REQ-F-041 — Редагування медикаменту (AD-13)

- **AC-F-041.1.** Адміністратор змінює назву або поріг → 200; змінено лише передане поле, залишок без змін.
  Перевіряє: `UpdateMedicationTest#renameMedication_changesOnlyName`, `UpdateMedicationTest#changeThreshold_changesOnlyThreshold`.
- **AC-F-041.2.** Нова назва збігається з чужою → 409 `MEDICATION_NAME_TAKEN`; поле `quantity` у запиті → 422 `IMMUTABLE_FIELD`.
  Перевіряє: `UpdateMedicationTest#renameToExistingName_returns409`, `UpdateMedicationTest#quantityInRequest_returns422ImmutableField`.

### REQ-F-042 — Ручне коригування залишку (AD-08)

- **AC-F-042.1.** Адміністратор задає ненульову зміну `delta` → 200, залишок = попередній + `delta`.
  Перевіряє: `AdjustMedicationStockTest#positiveDelta_increasesQuantity`, `AdjustMedicationStockTest#negativeDelta_decreasesQuantity`.
- **AC-F-042.2.** `delta = 0` → 422 `ZERO_DELTA`; абсолютне значення `quantity` замість зміни → 422 `UNKNOWN_FIELD`; паралельні коригування не втрачаються.
  Перевіряє: `AdjustMedicationStockTest#zeroDelta_returns422`, `AdjustMedicationStockTest#absoluteQuantity_returns422UnknownField`, `AdjustMedicationStockTest#concurrentAdjustments_areNotLost`.

### REQ-F-043 — Мінімальний пороговий залишок (AD-17)

- **AC-F-043.1.** Поріг задається під час створення чи редагування окремо для кожного медикаменту; без порога `minThreshold: null`; `null` очищає поріг.
  Перевіряє: `CreateMedicationTest#createWithoutThreshold_returnsNullThreshold`, `UpdateMedicationTest#changeThreshold_changesOnlyThreshold`, `UpdateMedicationTest#nullThreshold_clearsThreshold`.
- **AC-F-043.2.** `lowStock: true` тоді й лише тоді, коли залишок строго нижчий за поріг; фільтр `lowStock=true` повертає лише такі препарати.
  Перевіряє: `CreateMedicationTest#quantityBelowThreshold_marksLowStock`, `CreateMedicationTest#quantityEqualToThreshold_isNotLowStock`, `ListMedicationsTest#lowStockTrue_returnsOnlyLowStock`.

### REQ-F-044 — Медикаменти з каталогу у формі прийому (AD-06, AD-07)

- **AC-F-044.1.** Лікар бачить каталог медикаментів → 200.
  Перевіряє: `ListMedicationsTest#veterinarianListsMedications_returns200`.
- **AC-F-044.2.** Лікар зберігає прийом з одним або кількома препаратами каталогу й кількістю ≥ 1 → 201; у прийомі перелік `medicationId`, `medicationName`, `quantity`.
  Перевіряє: `CreateVisitTest#medications_areReturnedWithNames`, `CreateVisitStockTest#createVisit_decreasesEachMedication`.
- **AC-F-044.3.** Препарату немає в каталозі → 422 `REFERENCE_NOT_FOUND`; препарат указано двічі → 422 `DUPLICATE_MEDICATION`; кількість 0 чи від'ємна → 422 `OUT_OF_RANGE`; прийом не зберігається.
  Перевіряє: `CreateVisitValidationTest#unknownMedication_returns422ReferenceNotFound`, `CreateVisitValidationTest#duplicateMedication_returns422`, `CreateVisitValidationTest#zeroQuantity_returns422OutOfRange`.

### REQ-F-045 — Автоматичне списання при збереженні прийому (AD-07)

- **AC-F-045.1.** Після збереження прийому залишок кожного вказаного препарату зменшується рівно на вказану кількість (10 − 3 = 7).
  Перевіряє: `CreateVisitStockTest#createVisit_decreasesStock`, `CreateVisitStockTest#createVisit_decreasesEachMedication`.
- **AC-F-045.2.** Редагування прийому повторно не списує і не змінює перелік препаратів.
  Перевіряє: `UpdateVisitTest#update_keepsMedicationsAndStock`.

### REQ-F-047 — Сповіщення про низький залишок (AD-02, AD-16, AD-20)

- **AC-F-047.1.** Залишок після списання строго нижчий за поріг → створюється непрочитане сповіщення `LOW_STOCK` для адміністратора; рівний порогу, вищий або без порога → сповіщення немає.
  Перевіряє: `LowStockNotificationTest#quantityBelowThreshold_createsNotification`, `LowStockNotificationTest#quantityEqualToThreshold_createsNothing`, `LowStockNotificationTest#withoutThreshold_createsNothing`.
- **AC-F-047.2.** Поки є непрочитане сповіщення про препарат, нове не створюється; ручне коригування сповіщень не створює.
  Перевіряє: `LowStockNotificationTest#unreadNotificationExists_noDuplicate`, `LowStockNotificationTest#manualAdjustment_createsNothing`.
- **AC-F-047.3.** Адміністратор бачить сповіщення з фільтром `read` і позначає прочитаним → 200, `read: true`; лікар → 403.
  Перевіряє: `ListNotificationsTest#readFilter_separatesReadAndUnread`, `MarkNotificationReadTest#adminMarksRead_returns200`, `ListNotificationsTest#veterinarian_returns403`.

### REQ-F-048 — Залишок не від'ємний (AD-08)

- **AC-F-048.1.** Ручне коригування, після якого залишок став би менший за 0 → 409 `NEGATIVE_STOCK`, залишок не змінюється; зменшення рівно до 0 → 200.
  Перевіряє: `AdjustMedicationStockTest#deltaBelowZero_returns409AndKeepsQuantity`, `AdjustMedicationStockTest#deltaToExactlyZero_returns200`.
- **AC-F-048.2.** Від'ємний початковий залишок під час створення → 422 `OUT_OF_RANGE`.
  Перевіряє: `CreateMedicationValidationTest#negativeQuantity_returns422OutOfRange`.
- **AC-F-048.3.** Списання під час прийому не доводить залишок нижче 0, зокрема при паралельних прийомах.
  Перевіряє: `CreateVisitStockTest#emptyStock_returns409`, `CreateVisitStockTest#concurrentVisits_neverOverdrawStock`.

## 8. Нефункціональні вимоги

### REQ-NF-001 — Паролі лише як bcrypt-хеш, cost ≥ 10 (AD-11)

- **AC-NF-001.1.** Після створення облікового запису в базі зберігається bcrypt-хеш пароля з коефіцієнтом складності не нижче 10, а не сам пароль.
  Перевіряє: `PasswordStorageTest#createdUser_hasPasswordStoredAsBcryptHash`.
- **AC-NF-001.2.** Пароль ніколи не повертається у відповідях API.
  Перевіряє: `CreateUserTest#adminCreatesVeterinarianAccount_returns201WithoutPassword`, `GetUserTest#adminGetsCreatedUserByLocation_returns200WithoutPassword`.
- **AC-NF-001.3.** Пароль довший за 72 символи (межа bcrypt) відхиляється під час створення й входу → 422 `TOO_LONG`.
  Перевіряє: `CreateUserTest#adminCreatesUserWithTooLongPassword_returnsTooLongReason`, `LoginValidationTest#tooLongPassword_returns422TooLong`.

### REQ-NF-002 — 403 при недостатніх правах на кожному ендпоінті (AD-18)

- **AC-NF-002.1.** Для кожної з 32 операцій користувач із роллю поза `x-roles` отримує 403 `FORBIDDEN`, і дані не змінюються; анонімний запит → 401 `UNAUTHORIZED`.
  Перевіряє: тести ролей кожної операції, наприклад `UserAccessControlTest#veterinarianCreatesUser_returns403`, `PatientAccessAndContractTest#veterinarianToken_readsButCannotChangePatients`, `OwnerAccessAndContractTest#createOwnerWithoutToken_returns401`. Повну матрицю «операція × роль» (93 виклики) звірено під час виконання 2026-11-24 (`logs/api-docs-verification-2026-11-24.md`); окремого автоматичного тесту на всі 32 операції разом **немає**.
- **AC-NF-002.2.** Відповідь 403 має повну модель `Error`.
  Перевіряє: `ErrorModelTest#veterinarianRequestsAdminResource_returns403WithFullErrorModel`.

### REQ-NF-003 — Пошук ≤ 3 с при 10 000 записів (AD-15)

- **AC-NF-003.1.** Серед 10 000 пацієнтів пошук за підрядком клички, зокрема з фільтром власника, виконується швидше за 3 с.
  Перевіряє: `PatientSearchPerformanceTest#searchAmongTenThousandPatients_isFasterThanThreeSeconds`, `PatientSearchPerformanceTest#searchWithOwnerFilterAmongTenThousandPatients_isFasterThanThreeSeconds`.
- **AC-NF-003.2.** Серед 10 000 власників пошук за підрядком ПІБ і перша сторінка списку виконуються швидше за 3 с.
  Перевіряє: `OwnerSearchPerformanceTest#searchAmongTenThousandOwners_isFasterThanThreeSeconds`, `OwnerSearchPerformanceTest#listingAmongTenThousandOwners_isFasterThanThreeSeconds`.

### REQ-NF-004 — Тижневий розклад ≤ 2 с (AD-05, AD-17)

- **AC-NF-004.1.** Розклад лікаря за 7 днів із зайнятими слотами при 10 000 записах інших лікарів у базі повертається швидше за 2 с.
  Перевіряє: `SchedulePerformanceTest#weeklySchedule_isFasterThanTwoSeconds`.

### REQ-NF-005 — Атомарність прийому та списання (AD-07)

- **AC-NF-005.1.** Будь-яка помилка під час збереження прийому (нестача препарату або збій після списання) → жодних змін: прийому немає, запис `SCHEDULED`, залишки всіх препаратів і сповіщення як до запиту.
  Перевіряє: `CreateVisitStockTest#insufficientStock_changesNothing`, `VisitAtomicityTest#failureAfterWithdrawal_rollsEverythingBack`.
- **AC-NF-005.2.** Паралельні прийоми, що списують той самий препарат, не призводять до частково збереженого чи від'ємного стану.
  Перевіряє: `CreateVisitStockTest#concurrentVisits_neverOverdrawStock`.

### REQ-NF-006 — Журнал роботи та аудит (Addendum 003, AD-21…AD-24)

- **AC-NF-006.1.** Кожен HTTP-запит фіксується в технічному журналі (`application.log` і консоль) з методом, шляхом, кодом, тривалістю й `requestId`, без query-параметрів.
  Перевіряє: `HttpRequestLogTest#request_isLoggedWithMethodPathStatusDurationAndRequestId`, `HttpRequestLogTest#request_isLoggedWithoutQueryParameters`, `LogOutputTest#httpRequestLine_isWrittenToApplicationFile`, `LogOutputTest#rootLogger_hasConsoleAppender`.
- **AC-NF-006.2.** Кожен вхід (успішний і невдалий) і кожна зміна даних, зокрема відхилена, фіксується в журналі аудиту (`audit.log` і консоль) з дією, виконавцем, сутністю, результатом і кодом помилки; читання не фіксується.
  Перевіряє: `LoginAuditTest#successfulLogin_isAudited`, `AppointmentAuditTest#slotTaken_isAuditedAsFailure`, `UserDeactivationFailureAuditTest#deactivateTwice_isAuditedAsFailure`, `AppointmentAuditTest#readingSchedule_isNotAudited`.
- **AC-NF-006.3.** У жодному журналі немає паролів (відкритих чи хешованих), введеного при невдалому вході email, персональних даних власників і пацієнтів, діагнозів, призначень, нотаток і назв препаратів.
  Перевіряє: `UserAuditTest#createUser_doesNotLogPasswordOrHash`, `LoginAuditTest#failedLogin_doesNotLogEmailOrPassword`, `OwnerAuditTest#createOwner_isAuditedWithoutPersonalData`, `VisitAuditTest#medicalData_isNotLogged`.
- **AC-NF-006.4.** Файли журналів ротуються щодня і зберігаються 30 днів; збій запису в журнал аудиту не перериває бізнес-операцію.
  Перевіряє: `LogOutputTest#auditFile_rollsDailyAndKeeps30Days`, `LogOutputTest#failingAuditAppender_doesNotBreakLogin`.

---

## Підсумок покриття

| Показник | Значення |
|---|---:|
| Вимог у документі | 37 |
| Критеріїв | 84 |
| Критеріїв, повністю покритих автоматичними тестами | 82 |
| Критеріїв, покритих частково | 2: AC-NF-002.1 (тести ролей є для кожної операції окремо, загальну матрицю «операція × роль» перевірено під час виконання); AC-F-001.2 (незалежність email від регістру перевіряє тест бази даних, а відповідь API 409 — лише перевірка під час виконання) |

## Вплив на артефакти

Виконано після затвердження (2026-11-26):

- `spec/traceability_matrix.md`: колонка «Критерій прийняття» для 37 вимог посилається на критерії цього документа; оновлено примітку про критерії.
- `spec/srs.md`, `spec/api/openapi.yaml`, код і тести **не змінювались**.
- Запис у `/logs`: `logs/addendum-005-acceptance-criteria-2026-11-26.md`.
