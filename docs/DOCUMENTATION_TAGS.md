# Теги документації в проєкті vet-clinic-api

Документ описує теги, які **фактично** використовуються в коді та в контракті API. Усі приклади взято з репозиторію.

## 1. Де живе документація

| Шар | Де | Чим перевіряється |
|---|---|---|
| Java-код | Javadoc (`/** ... */`) у `src/main/java` і `src/test/java` | `./mvnw checkstyle:check` (`google_checks.xml`), `./mvnw javadoc:javadoc javadoc:test-javadoc` (doclint) |
| Контракт REST API | [`spec/api/openapi.yaml`](../spec/api/openapi.yaml) (OpenAPI 3, підхід design-first) | ручне рев'ю та тести контракту |

**Swagger/OpenAPI-анотацій у Java-коді немає.** У `pom.xml` немає залежностей `springdoc`/`io.swagger`, а в коді не трапляються `@Tag`, `@Operation`, `@ApiResponse`, `@Schema`, `@Parameter`. Контракт API ведеться окремим файлом `openapi.yaml`. Javadoc контролерів посилається на нього через `operationId` і REQ-ID, тому дублювати опис в анотаціях не потрібно (розділ 4).

Статистика Javadoc на 2026-11-24 (лише всередині блоків `/** */`; `src/main` включає 12 файлів `package-info.java`):

| Тег | `src/main` | `src/test` |
|---|---:|---:|
| Javadoc-блоків | 278 | 117 |
| `@param` | 418 | 45 |
| `@return` | 98 | 13 |
| `@throws` | 52 | 13 |
| `{@code ...}` | 309 | 23 |
| `{@link ...}` | 16 | 3 |
| `<p>` | 23 | 1 |

Теги `@see`, `@deprecated`, `@since`, `@author`, `{@inheritDoc}`, `{@literal}`, `{@linkplain}`, `<ul>` і `<pre>` у проєкті не використовуються.

## 2. Javadoc: блокові теги

Порядок блокових тегів фіксує правило Checkstyle `AtclauseOrder`: `@param` → `@return` → `@throws`. `NonEmptyAtclauseDescription` забороняє порожні описи. Рядок-продовження тегу має відступ 4 пробіли (`JavadocTagContinuationIndentation`).

### `@param` — параметр методу або компонент record

Для методів тег описує параметр і бізнес-обмеження, а не тип. Для `record` теги `@param` у Javadoc класу описують компоненти, тобто поля JSON-контракту.

```java
/**
 * Запис на прийом у відповідях API (схема {@code Appointment} в {@code openapi.yaml}).
 *
 * @param id ідентифікатор запису
 * @param doctorId лікар
 * ...
 * @param visitId прийом, створений за записом; {@code null}, поки запис не завершено
 * @param createdAt час створення
 * @param updatedAt час останньої зміни
 */
public record AppointmentResponse(
```
[`appointments/AppointmentResponse.java`](../src/main/java/ua/vetclinic/api/appointments/AppointmentResponse.java)

Правило Checkstyle `JavadocMethod` перевіряє, що кожен `@param` відповідає справжньому параметру. Пропустити `@param` дозволено (`allowMissingParamTags`), але в проєкті тег ставиться завжди, коли метод має Javadoc.

### `@return` — результат

```java
/**
 * Перевіряє, чи залишок після списання строго нижчий за поріг (AD-02.2).
 *
 * @return {@code true}, якщо потрібне сповіщення про низький залишок
 */
public boolean isLow() {
```
[`medications/WithdrawnMedication.java`](../src/main/java/ua/vetclinic/api/medications/WithdrawnMedication.java)

Опис не повинен починатися з «the …» і з малої латинської літери: правило `SummaryJavadoc` має `forbiddenSummaryFragments` з `^@return the *` і `^[a-z]`.

### `@throws` — бізнес-помилки

У сервісах `@throws` перелічує винятки разом зі стабільними кодами помилок, які бачить клієнт API. Так Javadoc узгоджується з відповідями 4xx в `openapi.yaml`.

```java
/**
 * Створює запис у статусі «Заплановано».
 *
 * @param request лікар, пацієнт і початок слота
 * @return створений запис
 * @throws RequestValidationException якщо лікаря чи пацієнта не знайдено, лікаря деактивовано або
 *     слот не відповідає сітці, робочим дням чи годинам клініки (422)
 * @throws ConflictException {@code PATIENT_ARCHIVED} для архівної картки або {@code SLOT_TAKEN},
 *     якщо слот уже зайнятий (409)
 */
AppointmentResponse create(CreateAppointmentRequest request) {
```
[`appointments/AppointmentService.java`](../src/main/java/ua/vetclinic/api/appointments/AppointmentService.java)

Якщо клас винятку не імпортовано у файл, у тегу пишеться повне ім'я, щоб doclint зміг знайти посилання:

```java
 * @throws java.util.NoSuchElementException якщо запису немає
```
[`appointments/Appointments.java`](../src/main/java/ua/vetclinic/api/appointments/Appointments.java)

Перевірені винятки Java також документуються (`SecurityConfig.securityFilterChain`, тестові хелпери):

```java
 * @throws Exception якщо налаштування не вдалося зібрати
```
[`security/SecurityConfig.java`](../src/main/java/ua/vetclinic/api/security/SecurityConfig.java)

## 3. Javadoc: вбудовані теги та HTML

### `{@code ...}` — код, значення, коди помилок

Так позначаються імена полів JSON, значення enum, коди помилок, URL-шляхи й фрагменти коду. Текст усередині не розбирається як HTML, тому символи `<` і `>` безпечні:

```java
 * @return {@code Bearer <токен>}
```
[`TestVisits.java`](../src/test/java/ua/vetclinic/api/TestVisits.java)

```java
/**
 * REST-ресурс власників {@code /api/v1/owners}.
 * ...
 */
```
[`owners/OwnerController.java`](../src/main/java/ua/vetclinic/api/owners/OwnerController.java)

### `{@link ...}` — посилання на інший тип або член

```java
 * <p>Перед архівацією опитуються всі {@link PatientArchiveGuard}; поки є заплановані записи,
 * архівація заборонена (AD-04.2).
```
[`patients/PatientService.java`](../src/main/java/ua/vetclinic/api/patients/PatientService.java)

Посилання на член класу і на константу enum:

```java
 * <p>Запис займає рівно один 30-хвилинний слот лікаря. Статус змінюється лише методами {@link
 * #cancel}, {@link #complete} і {@link #reschedule}; фізично запис не видаляється.
```
[`appointments/AppointmentEntity.java`](../src/main/java/ua/vetclinic/api/appointments/AppointmentEntity.java)

```java
  /**
   * Обрізає пробіли в текстових полях і підставляє {@link Sex#UNKNOWN} замість відсутньої статі.
   */
```
[`patients/CreatePatientRequest.java`](../src/main/java/ua/vetclinic/api/patients/CreatePatientRequest.java)

Doclint перевіряє, що кожне посилання існує, тому перейменування класу без оновлення `{@link}` зламає `javadoc:javadoc`.

### `<p>` — новий абзац

За Google Java Style абзаци відокремлюються порожнім рядком, а `<p>` стоїть одразу перед першим словом абзацу без пробілу (правило `JavadocParagraph`, `allowNewlineParagraph=false`):

```java
/**
 * Бізнес-логіка записів на прийом: перевірка слота, лікаря й пацієнта, захист від конфлікту
 * розкладу та фіксація кожної зміни в журналі аудиту.
 *
 * <p>Конфлікт розкладу перевіряється двічі: запитом перед збереженням і унікальним індексом БД,
 * який ловить паралельні запити на той самий слот.
 */
```
[`appointments/AppointmentService.java`](../src/main/java/ua/vetclinic/api/appointments/AppointmentService.java)

### Однорядковий Javadoc

Якщо коментар без тегів і вміщується в рядок, використовується форма `/** ... */` (`SingleLineJavadoc` забороняє блокові теги в ній):

```java
  /** Обрізає пробіли по краях назви (AD-17.1). */
  public CreateMedicationRequest {
```
[`medications/CreateMedicationRequest.java`](../src/main/java/ua/vetclinic/api/medications/CreateMedicationRequest.java)

## 4. Конвенції змісту Javadoc у проєкті

1. **Мова — українська**, терміни відповідають глосарію SRS §3: Власник, Пацієнт, Запис на прийом, Прийом, Слот, Залишок препарату тощо.
2. **Перше речення — підсумок.** Воно закінчується крапкою, починається з дієслова в 3-й особі («Створює…», «Повертає…») або з іменника для класів («REST-ресурс…», «JPA-сутність…»).
3. **Простежуваність.** Коментарі посилаються на вимоги SRS (`REQ-F-023`) і рішення адендумів (`AD-04.2`).
4. **Контролери** називають `operationId` з `openapi.yaml`, наприклад:
   ```java
   * Створює запис на вільний слот лікаря (операція {@code createAppointment}, REQ-F-021–023,
   * REQ-F-049).
   ```
   Повний опис помилок і прикладів лишається в контракті.
5. **Сервіси** документують бізнес-обмеження та `@throws` з кодами помилок.
6. **DTO (`record`)** документують кожен компонент через `@param` у Javadoc класу.
7. **Без механічних коментарів.** Не документуються:
   - DI-конструктори;
   - захищені no-arg конструктори JPA;
   - тривіальні аксесори сутностей;
   - похідні запити Spring Data із самоописними назвами (`existsByEmail`);
   - методи з `@Override`.

   Checkstyle дозволяє їх пропускати (`allowMissingPropertyJavadoc`, `allowedAnnotations=Override`, область перевірки `protected`).
8. **Сеттери PATCH-запитів** (`UpdateOwnerRequest.setEmail` тощо) документуються попри простоту. Вони не тривіальні: обрізають пробіли й позначають поле переданим, щоб відрізнити відсутнє поле від явного `null`.

## 5. Як перевірити

```bash
./mvnw checkstyle:check                       # наявність і стиль Javadoc (Google Java Style)
./mvnw javadoc:javadoc javadoc:test-javadoc   # синтаксис тегів, HTML і посилання (doclint)
./mvnw spotless:apply                         # форматування, зокрема переносів у Javadoc
```

`maven-javadoc-plugin` налаштовано в `pom.xml` так:
- `show=package` — генеруються й перевіряються також package-private класи (контролери, сервіси, сутності);
- `doclint=all,-missing` — перевіряються синтаксис, HTML, посилання й теги, а наявність коментарів лишається на Checkstyle;
- `failOnWarnings=true` — будь-яке попередження doclint зупиняє збірку.
