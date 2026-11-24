# Приведення коду до Google Java Style Guide — 2026-11-24

## 1. Налаштування

У `pom.xml` додано:

| Плагін | Версія | Призначення |
|---|---|---|
| `maven-checkstyle-plugin` + `com.puppycrawl.tools:checkstyle` | 3.6.0 / 14.4.0 | перевірка за вбудованою конфігурацією `google_checks.xml` (main + test) |
| `spotless-maven-plugin` + `google-java-format` | 3.12.0 / 2.0.0 | автоматичне переформатування (стиль `GOOGLE`, `removeUnusedImports`) |

Checkstyle налаштовано з `violationSeverity=warning` (усі правила `google_checks.xml` мають рівень warning) і `failOnViolation=false`, поки не дописано Javadoc. Плагін не прив'язаний до жодної фази, тому `mvn package` поводиться як раніше.

Команди:

```bash
./mvnw checkstyle:check     # перевірка стилю
./mvnw spotless:apply       # переформатування
./mvnw spotless:check       # перевірка форматування (для CI)
```

## 2. Результати Checkstyle

| Правило | ДО | ПІСЛЯ |
|---|---:|---:|
| FileTabCharacter (табуляція замість пробілів) | 13 842 | 0 |
| Indentation (відступи не 2/4 пробіли) | 6 702 | 0 |
| LineLength (> 100 символів) | 1 494 | 0 |
| TextBlockGoogleStyleFormatting (текстові блоки `"""`) | 432 | 0 |
| CustomImportOrder (порядок і групи імпортів) | 353 | 0 |
| whitespaceCharactersMustBeEscaped (табуляції в текстових блоках) | 144 | 0 |
| GoogleRightCurly (розміщення `}`) | 9 | 0 |
| VariableDeclarationUsageDistance | 3 | 0 |
| OverloadMethodsDeclarationOrder | 3 | 0 |
| SummaryJavadoc | 1 | 0 |
| **Разом стильових** | **22 983** | **0** |
| MissingJavadocMethod | 70 | 70 |
| MissingJavadocType | 40 | 40 |
| **Усього** | **23 093** | **110** |

110 порушень, що лишились, стосуються тільки відсутнього Javadoc (87 у `src/main`, 23 у `src/test`). Їх відкладено на наступний крок.

Wildcard-імпортів (`AvoidStarImport`) і порушень іменування (`*Name`) у проєкті не знайдено ні до, ні після.

## 3. Що виправлено

1. **Автоматично (google-java-format через Spotless, 196 файлів):** табуляції замінено на 2 пробіли, продовження рядків мають відступ +4, імпорти зібрано в одну групу без порожніх рядків у лексикографічному порядку, вирівняно текстові блоки й розміщення фігурних дужок, переноси до 100 символів.
2. **Довгі рядкові літерали (118 рядків, переважно `@DisplayName` українською та один шаблон лог-повідомлення в `AuditLog`):** розбито на конкатенацію `"…" + "…"`. Це константа часу компіляції, тож значення рядка не змінилось.
3. **OverloadMethodsDeclarationOrder:** перевантаження `success`/`failure` в `AuditLog` і `reasonOf` в `ApiExceptionHandler` розміщено поруч (методи лише переставлено).
4. **VariableDeclarationUsageDistance:**
   - `MedicationStock.withdraw`: оголошення `message` перенесено ближче до місця використання (обчислення не має побічних ефектів);
   - у двох тестах (`AppointmentAuditTest`, `PatientAccessAndContractTest`) значення мусять обчислюватися до фази Act, тому замість перенесення змінні позначено `final` (як радить Checkstyle).
5. **SummaryJavadoc:** у `LoginValidationTest` `openapi.yaml` обгорнуто в `{@code …}`, бо крапка в імені файлу сприймалась як кінець речення.

## 4. Перевірка незмінності поведінки

- Сигнатури публічних методів, URL-шляхи ендпоінтів, DTO і JSON-контракти не змінювались.
- Байт-код `src/main` до (HEAD) і після зміни скомпільовано з `-g:none` і порівняно через `javap -c -p -constants`. Для 110 зі 113 класів він ідентичний. У трьох класах відрізняються лише номери в пулі констант через перестановку методів і, в `MedicationStock`, порядок обчислення рядка `message`. Вміст рядкових констант, зокрема SQL/JPQL у текстових блоках, не змінився.

## 5. Тести

Команда `./mvnw test` (Testcontainers + PostgreSQL) після всіх змін:

```
[INFO] Tests run: 745, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Рядок `ERROR … UnexpectedExceptionHandler` у лозі очікуваний: `VisitAtomicityTest` навмисно викликає збій, щоб перевірити відкат транзакції.
