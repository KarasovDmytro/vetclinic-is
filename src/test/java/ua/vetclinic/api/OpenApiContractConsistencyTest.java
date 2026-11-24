package ua.vetclinic.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

/**
 * Звіряє {@code spec/api/openapi.yaml} з ендпоінтами, які фактично зареєстровані в контексті Spring
 * MVC: шляхи й методи, {@code operationId}, параметри та поля DTO (разом із вкладеними record і
 * значеннями enum).
 *
 * <p>Окрім перевірки, експортує фактичну карту ендпоінтів у {@code
 * target/openapi-check/endpoints.md}, щоб її можна було порівняти з документацією.
 */
@ApiIntegrationTest
class OpenApiContractConsistencyTest {

  private static final Path SPEC = Path.of("spec/api/openapi.yaml");
  private static final Path EXPORT = Path.of("target/openapi-check/endpoints.md");
  private static final String BASE_PATH = "/api/v1";
  private static final String SCHEMA_REF = "#/components/schemas/";
  private static final String PARAMETER_REF = "#/components/parameters/";

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping handlerMapping;

  private Map<String, Object> spec;
  private Map<String, Endpoint> actual;

  /** Ендпоінт застосунку: HTTP-метод, шлях відносно {@code /api/v1} і метод контролера. */
  private record Endpoint(String method, String path, HandlerMethod handler) {
    String key() {
      return method + " " + path;
    }
  }

  @BeforeEach
  void loadSpecAndMappings() throws IOException {
    try (InputStream in = Files.newInputStream(SPEC)) {
      spec = new Yaml().load(in);
    }
    actual = new TreeMap<>();
    for (Map.Entry<RequestMappingInfo, HandlerMethod> entry :
        handlerMapping.getHandlerMethods().entrySet()) {
      HandlerMethod handler = entry.getValue();
      if (!handler.getBeanType().getPackageName().startsWith("ua.vetclinic.api")) {
        continue;
      }
      for (String pattern : entry.getKey().getPatternValues()) {
        for (var method : entry.getKey().getMethodsCondition().getMethods()) {
          Endpoint endpoint =
              new Endpoint(method.name(), pattern.substring(BASE_PATH.length()), handler);
          actual.put(endpoint.key(), endpoint);
        }
      }
    }
  }

  @Test
  @DisplayName("Набір «метод + шлях» у застосунку збігається з paths в openapi.yaml")
  void endpoints_matchSpecPaths() throws IOException {
    // Arrange
    Set<String> documented = new TreeSet<>(specOperations().keySet());

    // Act
    exportEndpoints();

    // Assert
    assertThat(actual.keySet())
        .as("ендпоінти без опису в openapi.yaml або описані, але не реалізовані")
        .containsExactlyInAnyOrderElementsOf(documented);
  }

  @Test
  @DisplayName("operationId кожної операції дорівнює назві методу контролера")
  void operationIds_matchControllerMethodNames() {
    // Arrange
    Map<String, Map<String, Object>> operations = specOperations();
    List<String> mismatches = new ArrayList<>();

    // Act
    actual.forEach((key, endpoint) -> {
      Object operationId = operations.getOrDefault(key, Map.of()).get("operationId");
      if (!endpoint.handler().getMethod().getName().equals(operationId)) {
        mismatches.add(key + ": " + operationId + " ≠ " + endpoint.handler().getMethod().getName());
      }
    });

    // Assert
    assertThat(mismatches).isEmpty();
  }

  @Test
  @DisplayName("Параметри шляху й запиту контролерів збігаються з parameters операцій")
  void parameters_matchSpec() {
    // Arrange
    Map<String, Map<String, Object>> operations = specOperations();
    List<String> mismatches = new ArrayList<>();
    DefaultParameterNameDiscoverer names = new DefaultParameterNameDiscoverer();

    // Act
    actual.forEach((key, endpoint) -> {
      Set<String> javaParams = new TreeSet<>();
      for (MethodParameter parameter : endpoint.handler().getMethodParameters()) {
        parameter.initParameterNameDiscovery(names);
        RequestParam query = parameter.getParameterAnnotation(RequestParam.class);
        PathVariable path = parameter.getParameterAnnotation(PathVariable.class);
        if (query != null) {
          javaParams.add("query:" + nameOr(query.name(), parameter));
        } else if (path != null) {
          javaParams.add("path:" + nameOr(path.name(), parameter));
        }
      }
      Set<String> specParams = new TreeSet<>();
      for (Object raw : parametersOf(key, operations.get(key))) {
        Map<String, Object> parameter = resolveParameter(raw);
        specParams.add(parameter.get("in") + ":" + parameter.get("name"));
      }
      if (!javaParams.equals(specParams)) {
        mismatches.add(key + ": код " + javaParams + ", специфікація " + specParams);
      }
    });

    // Assert
    assertThat(mismatches).isEmpty();
  }

  @Test
  @DisplayName("Поля DTO запитів і відповідей (з вкладеними record та enum) збігаються зі схемами")
  void dtoFields_matchSchemas() {
    // Arrange
    Map<String, Map<String, Object>> operations = specOperations();
    List<String> mismatches = new ArrayList<>();

    // Act
    actual.forEach((key, endpoint) -> {
      Map<String, Object> operation = operations.get(key);
      Method method = endpoint.handler().getMethod();
      Map<String, Object> responseSchema = successSchema(operation);
      if (responseSchema != null) {
        compare(
            key + " відповідь", method.getGenericReturnType(), responseSchema, true, mismatches);
      }
      Map<String, Object> requestSchema = requestSchema(operation);
      for (MethodParameter parameter : endpoint.handler().getMethodParameters()) {
        if (parameter.hasParameterAnnotation(RequestBody.class)) {
          compare(
              key + " тіло", parameter.getGenericParameterType(), requestSchema, false, mismatches);
        }
      }
    });

    // Assert
    assertThat(mismatches).isEmpty();
  }

  /**
   * Порівнює Java-тип зі схемою. Для record поля мають збігатися точно; для PATCH-класів із
   * сеттерами схема має бути підмножиною сеттерів, бо незмінювані поля приймаються, щоб їх
   * відхилити з причиною {@code IMMUTABLE_FIELD}.
   */
  private void compare(
      String where, Type type, Map<String, Object> schema, boolean exact, List<String> mismatches) {
    if (schema == null) {
      mismatches.add(where + ": у специфікації немає схеми");
      return;
    }
    Type unwrapped = unwrap(type);
    Map<String, Object> resolved = effective(schema);
    if (unwrapped instanceof ParameterizedType generic
        && Collection.class.isAssignableFrom((Class<?>) generic.getRawType())) {
      compare(
          where + "[]",
          generic.getActualTypeArguments()[0],
          castMap(resolved.get("items")),
          exact,
          mismatches);
      return;
    }
    if (!(unwrapped instanceof Class<?> clazz)
        || !clazz.getPackageName().startsWith("ua.vetclinic.api")) {
      return;
    }
    if (clazz.isEnum()) {
      Set<String> javaValues = new TreeSet<>();
      Arrays.stream(clazz.getEnumConstants()).forEach(value -> javaValues.add(value.toString()));
      Set<String> specValues = new TreeSet<>();
      ((List<?>) resolved.get("enum")).forEach(value -> specValues.add(value.toString()));
      if (!javaValues.equals(specValues)) {
        mismatches.add(where + " enum: код " + javaValues + ", специфікація " + specValues);
      }
      return;
    }
    Map<String, Map<String, Object>> specProps = propertiesOf(resolved);
    Map<String, Type> javaProps = propertiesOf(clazz);
    boolean matches =
        exact || clazz.isRecord()
            ? javaProps.keySet().equals(specProps.keySet())
            : javaProps.keySet().containsAll(specProps.keySet());
    if (!matches) {
      mismatches.add(
          where
              + " ("
              + clazz.getSimpleName()
              + "): код "
              + new TreeSet<>(javaProps.keySet())
              + ", специфікація "
              + new TreeSet<>(specProps.keySet()));
      return;
    }
    specProps.forEach((name, property) ->
        compare(where + "." + name, javaProps.get(name), property, exact, mismatches));
  }

  /** Розкриває {@code $ref} і обгортку {@code allOf} з одним елементом (nullable-посилання). */
  private Map<String, Object> effective(Map<String, Object> schema) {
    Map<String, Object> resolved = resolveSchema(schema);
    Object allOf = resolved.get("allOf");
    if (allOf instanceof List<?> parts
        && parts.size() == 1
        && !resolved.containsKey("properties")) {
      return effective(castMap(parts.getFirst()));
    }
    return resolved;
  }

  private static Map<String, Type> propertiesOf(Class<?> clazz) {
    Map<String, Type> properties = new LinkedHashMap<>();
    if (clazz.isRecord()) {
      for (RecordComponent component : clazz.getRecordComponents()) {
        properties.put(component.getName(), component.getGenericType());
      }
      return properties;
    }
    for (Method method : clazz.getMethods()) {
      if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
        String name = method.getName().substring(3);
        properties.put(
            Character.toLowerCase(name.charAt(0)) + name.substring(1),
            method.getGenericParameterTypes()[0]);
      }
    }
    return properties;
  }

  private Map<String, Map<String, Object>> propertiesOf(Map<String, Object> schema) {
    Map<String, Map<String, Object>> properties = new LinkedHashMap<>();
    Object allOf = schema.get("allOf");
    if (allOf != null) {
      for (Object part : (List<?>) allOf) {
        properties.putAll(propertiesOf(resolveSchema(castMap(part))));
      }
    }
    Object own = schema.get("properties");
    if (own != null) {
      castMap(own).forEach((name, value) -> properties.put(name, castMap(value)));
    }
    return properties;
  }

  private static Type unwrap(Type type) {
    if (type instanceof ParameterizedType generic && generic.getRawType() == ResponseEntity.class) {
      return generic.getActualTypeArguments()[0];
    }
    return type;
  }

  private Map<String, Map<String, Object>> specOperations() {
    Map<String, Map<String, Object>> operations = new TreeMap<>();
    castMap(spec.get("paths"))
        .forEach((path, item) ->
            castMap(item)
                .forEach((method, operation) -> {
                  if (Set.of("get", "post", "put", "patch", "delete").contains(method)) {
                    operations.put(method.toUpperCase() + " " + path, castMap(operation));
                  }
                }));
    return operations;
  }

  private List<?> parametersOf(String key, Map<String, Object> operation) {
    List<Object> parameters = new ArrayList<>();
    String path = key.substring(key.indexOf(' ') + 1);
    Object shared = castMap(castMap(spec.get("paths")).get(path)).get("parameters");
    if (shared != null) {
      parameters.addAll((List<?>) shared);
    }
    Object own = operation.get("parameters");
    if (own != null) {
      parameters.addAll((List<?>) own);
    }
    return parameters;
  }

  private Map<String, Object> successSchema(Map<String, Object> operation) {
    Map<String, Object> responses = castMap(operation.get("responses"));
    for (String code : List.of("200", "201")) {
      if (responses.containsKey(code)) {
        return jsonSchema(castMap(responses.get(code)));
      }
    }
    return null;
  }

  private Map<String, Object> requestSchema(Map<String, Object> operation) {
    Object body = operation.get("requestBody");
    return body == null ? null : jsonSchema(castMap(body));
  }

  private static Map<String, Object> jsonSchema(Map<String, Object> holder) {
    Object content = holder.get("content");
    if (content == null) {
      return null;
    }
    return castMap(castMap(castMap(content).get("application/json")).get("schema"));
  }

  private Map<String, Object> resolveSchema(Map<String, Object> schema) {
    Object ref = schema.get("$ref");
    if (ref == null) {
      return schema;
    }
    String name = ref.toString().substring(SCHEMA_REF.length());
    return resolveSchema(
        castMap(castMap(castMap(spec.get("components")).get("schemas")).get(name)));
  }

  private Map<String, Object> resolveParameter(Object raw) {
    Map<String, Object> parameter = castMap(raw);
    Object ref = parameter.get("$ref");
    if (ref == null) {
      return parameter;
    }
    String name = ref.toString().substring(PARAMETER_REF.length());
    return castMap(castMap(castMap(spec.get("components")).get("parameters")).get(name));
  }

  private static String nameOr(String declared, MethodParameter parameter) {
    return declared.isEmpty() ? parameter.getParameterName() : declared;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> castMap(Object value) {
    return (Map<String, Object>) value;
  }

  private void exportEndpoints() throws IOException {
    Map<String, Map<String, Object>> operations = specOperations();
    List<String> lines = new ArrayList<>();
    lines.add("# Ендпоінти, зареєстровані в контексті Spring MVC");
    lines.add("");
    lines.add(
        "Базовий шлях: `"
            + BASE_PATH
            + "`. Згенеровано тестом `"
            + getClass().getSimpleName()
            + "`.");
    lines.add("");
    lines.add("| Метод | Шлях | operationId | Контролер | Резюме з openapi.yaml |");
    lines.add("|---|---|---|---|---|");
    Set<String> controllers = new LinkedHashSet<>();
    actual.values().stream()
        .sorted((a, b) ->
            a.path().equals(b.path())
                ? a.method().compareTo(b.method())
                : a.path().compareTo(b.path()))
        .forEach(endpoint -> {
          HandlerMethod handler = endpoint.handler();
          controllers.add(handler.getBeanType().getName());
          Map<String, Object> operation = operations.getOrDefault(endpoint.key(), Map.of());
          lines.add(
              "| "
                  + endpoint.method()
                  + " | `"
                  + endpoint.path()
                  + "` | `"
                  + operation.get("operationId")
                  + "` | `"
                  + handler.getBeanType().getSimpleName()
                  + "#"
                  + handler.getMethod().getName()
                  + "` | "
                  + operation.get("summary")
                  + " |");
        });
    lines.add("");
    lines.add("Контролерів: " + controllers.size() + ", ендпоінтів: " + actual.size() + ".");
    Files.createDirectories(EXPORT.getParent());
    Files.write(EXPORT, lines, StandardCharsets.UTF_8);
  }
}
