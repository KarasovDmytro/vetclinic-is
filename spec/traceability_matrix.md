# Матриця простежуваності вимог

| ID вимоги | Короткий опис | Критерій прийняття | BDD-сценарій | Тікет | Тест | Код | Документація |
|---|---|---|---|---|---|---|---|
| REQ-F-001 | Створення облікового запису (Адміністратор/Лікар) адміністратором | - | - | TSK-002, TSK-004, TSK-003, TSK-015 | TBD | TBD | `createUser`, `listUsers`, `getUser`; AD-01, AD-11, AD-17 |
| REQ-F-002 | Деактивація облікового запису адміністратором | - | - | TSK-004, TSK-003, TSK-015 | TBD | TBD | `deactivateUser`; AD-01, AD-16, AD-20 |
| REQ-F-003 | Деактивований користувач не може увійти | - | - | TSK-002, TSK-004, TSK-003, TSK-015 | TBD | TBD | `login`; AD-01 |
| REQ-F-004 | Доступ виключно за роллю користувача | є | - | TSK-004, TSK-003, TSK-015 | TBD | TBD | усі операції (`x-cross-cutting-requirements`); AD-18, AD-19 |
| REQ-F-005 | Лікар не має доступу до управління обліковими записами | - | - | TSK-004, TSK-003, TSK-015 | TBD | TBD | `createUser`, `listUsers`, `getUser`, `deactivateUser`; AD-06 |
| REQ-F-010 | Реєстрація нового власника | - | - | TSK-006, TSK-005, TSK-015 | TBD | TBD | `createOwner`, `getOwner`; AD-10, AD-17 |
| REQ-F-011 | Редагування даних власника | - | - | TSK-006, TSK-005, TSK-015 | TBD | TBD | `updateOwner`; AD-10, AD-17 |
| REQ-F-012 | Реєстрація пацієнта з прив'язкою до власника | - | - | TSK-006, TSK-005, TSK-015 | TBD | TBD | `createPatient`, `getPatient`; AD-10, AD-17 |
| REQ-F-013 | Редагування базових даних пацієнта | - | - | TSK-006, TSK-005, TSK-015 | TBD | TBD | `updatePatient`; AD-10, AD-17 |
| REQ-F-014 | Пацієнт прив'язаний до рівно одного власника; зміна власника не передбачена | - | - | TSK-006, TSK-005, TSK-015 | TBD | TBD | `createPatient`, `updatePatient`; AD-10, AD-19 |
| REQ-F-015 | Переведення медичної картки пацієнта в статус «Архівна» | - | - | TSK-006, TSK-005, TSK-015 | TBD | TBD | `archivePatient`; AD-04, AD-16 |
| REQ-F-016 | Заборона фізичного видалення картки | є | - | TSK-006, TSK-005, TSK-015 | TBD | TBD | `getPatient`, `archivePatient`; AD-09, AD-18 |
| REQ-F-017 | Пошук власників та пацієнтів за іменем | - | - | TSK-006, TSK-005, TSK-015 | TBD | TBD | `listOwners`, `listPatients`; AD-15 |
| REQ-F-020 | Розклад у вигляді сітки зі слотами по 30 хвилин | - | - | TSK-010, TSK-009, TSK-015 | TBD | TBD | `listDoctors`, `getDoctorSchedule`, `listAppointments`; AD-05, AD-17, AD-20 |
| REQ-F-021 | Створення запису на прийом адміністратором | - | - | TSK-002, TSK-010, TSK-009, TSK-015 | TBD | TBD | `listDoctors`, `createAppointment`, `getAppointment`; AD-04, AD-05, AD-17 |
| REQ-F-022 | Автоматичний статус «Заплановано» при створенні запису | - | - | TSK-010, TSK-009, TSK-015 | TBD | TBD | `createAppointment`; AD-19 |
| REQ-F-023 | Блокування конфлікту розкладу | є | є | TSK-002, TSK-010, TSK-009, TSK-015 | TBD | TBD | `createAppointment`, `rescheduleAppointment`; AD-03, AD-18 |
| REQ-F-024 | Перенесення запису на інший вільний слот | - | - | TSK-002, TSK-010, TSK-009, TSK-015 | TBD | TBD | `rescheduleAppointment`; AD-03, AD-20 |
| REQ-F-025 | Адміністратор може скасувати запис | - | - | TSK-002, TSK-010, TSK-009, TSK-015 | TBD | TBD | `cancelAppointment`; AD-03, AD-16, AD-20 |
| REQ-F-026 | Лікар лише переглядає розклад, не змінює записи | - | - | TSK-010, TSK-009, TSK-015 | TBD | TBD | `getDoctorSchedule`, `listAppointments`, `rescheduleAppointment`, `cancelAppointment`; AD-06 |
| REQ-F-027 | Автоматичний статус «Завершено» при збереженні прийому | - | - | TSK-012, TSK-011, TSK-015 | TBD | TBD | `createVisit`; AD-07, AD-19 |
| REQ-F-030 | Лікар бачить картки всіх пацієнтів клініки | - | - | TSK-012, TSK-011, TSK-015 | TBD | TBD | `listPatientVisits`, `getVisit`; AD-06 |
| REQ-F-031 | Адміністратор не має доступу до медичних карток | - | - | TSK-012, TSK-011, TSK-015 | TBD | TBD | `listPatientVisits`, `createVisit`, `getVisit`, `updateVisit`; AD-18 |
| REQ-F-032 | Лікар фіксує результати прийому (діагноз, призначення, нотатки) | - | - | TSK-012, TSK-011, TSK-015 | TBD | TBD | `createVisit`, `updateVisit`; AD-07, AD-12 |
| REQ-F-033 | Прийом містить дату, ідентифікатор лікаря та пацієнта | - | - | TSK-012, TSK-011, TSK-015 | TBD | TBD | `createVisit`, `getVisit`; AD-14 |
| REQ-F-034 | Перегляд повної хронології прийомів пацієнта | - | - | TSK-012, TSK-011, TSK-015 | TBD | TBD | `listPatientVisits`, `getVisit`; AD-14 |
| REQ-F-035 | Архівна картка — лише перегляд для лікаря | є | - | TSK-012, TSK-011, TSK-015 | TBD | TBD | `listPatientVisits`, `createVisit`, `updateVisit`; AD-04, AD-07 |
| REQ-F-040 | Додавання медикаменту до каталогу | - | - | TSK-008, TSK-007, TSK-015 | TBD | TBD | `createMedication`, `getMedication`; AD-13, AD-17 |
| REQ-F-041 | Редагування даних медикаменту в каталозі | - | - | TSK-008, TSK-007, TSK-015 | TBD | TBD | `updateMedication`; AD-13 |
| REQ-F-042 | Ручне коригування залишку медикаменту адміністратором | - | - | TSK-008, TSK-007, TSK-015 | TBD | TBD | `adjustMedicationStock`; AD-08 |
| REQ-F-043 | Встановлення мінімального порогового залишку | - | - | TSK-008, TSK-007, TSK-015 | TBD | TBD | `createMedication`, `updateMedication`; AD-17 |
| REQ-F-044 | Додавання медикаментів із каталогу до форми прийому з кількістю | - | - | TSK-014, TSK-013, TSK-015 | TBD | TBD | `createVisit`, `listMedications`; AD-06, AD-07 |
| REQ-F-045 | Автоматичне зменшення залишку при збереженні прийому | - | - | TSK-014, TSK-013, TSK-015 | TBD | TBD | `createVisit`; AD-07 |
| REQ-F-046 | Блокування збереження при нестачі медикаменту | є | є | TSK-014, TSK-013, TSK-015 | TBD | TBD | `createVisit`; AD-08, AD-18 |
| REQ-F-047 | Сповіщення адміністратора при падінні нижче порогу | - | - | TSK-002, TSK-014, TSK-013, TSK-015 | TBD | TBD | `createVisit`, `listNotifications`, `markNotificationRead`; AD-02, AD-16, AD-20 |
| REQ-F-048 | Залишок медикаменту не може бути від'ємним | - | - | TSK-014, TSK-013, TSK-015 | TBD | TBD | `createVisit`, `adjustMedicationStock`; AD-08 |
| REQ-NF-001 | Хешування паролів bcrypt (cost ≥ 10) | - | - | TSK-004, TSK-003, TSK-015 | TBD | TBD | `login`, `createUser`; AD-11 |
| REQ-NF-002 | HTTP 403 при недостатніх правах на кожному ендпоінті | - | - | TSK-004, TSK-003, TSK-015 | TBD | TBD | усі операції (`x-cross-cutting-requirements`); AD-18 |
| REQ-NF-003 | Пошук за іменем ≤ 3 с при 10 000 пацієнтів | - | - | TSK-006, TSK-005, TSK-015 | TBD | TBD | `listOwners`, `listPatients`; AD-15 |
| REQ-NF-004 | Завантаження тижневого розкладу ≤ 2 с | - | - | TSK-010, TSK-009, TSK-015 | TBD | TBD | `getDoctorSchedule`; AD-05, AD-17 |
| REQ-NF-005 | Атомарна транзакція: прийом + списання | - | - | TSK-014, TSK-013, TSK-015 | TBD | TBD | `createVisit`; AD-07 |

## Примітки

- **Документація.** Значення колонки це `operationId` операцій у `spec/api/openapi.yaml` (v1.0.0), які реалізують вимогу (за розширенням `x-requirements`), та рішення адендумів, що визначають поведінку, якої немає в SRS: AD-01…AD-09 у `spec/addendum-001-api-decisions.md`, AD-10…AD-20 у `spec/addendum-002-api-contract-decisions.md`.
- **Наскрізні вимоги.** REQ-F-004 і REQ-NF-002 діють для всіх операцій (`x-cross-cutting-requirements`), тому окремий `operationId` для них не вказується.
- **Вимоги без окремої операції.** REQ-F-014, REQ-F-016, REQ-F-022, REQ-F-033, REQ-F-048 і REQ-NF-001 реалізуються схемою або поведінкою (незмінний `ownerId`, відсутність `DELETE`, статус від сервера, поля `Visit`, невід'ємний залишок, пароль лише на запис) і зазначені біля відповідних операцій.
- **Тест, Код.** Залишаються `TBD` до виконання етапів TDD (TSK-003…TSK-014).
- **Джерело рішення про затвердження.** API-GATE: `logs/api-gate-2026-10-29.md` (GO, 2026-10-29).
