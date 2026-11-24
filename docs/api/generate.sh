#!/usr/bin/env bash
# Генерує API-довідник проєкту vet-clinic-api:
#   docs/api/rest/index.html - довідник REST API з spec/api/openapi.yaml (Redocly CLI, Redoc);
#   docs/api/javadoc/        - довідник Java-коду з Javadoc (maven-javadoc-plugin, doclint).
# Потрібні: Docker (образ redocly/cli) і JDK для ./mvnw. Запуск із кореня репозиторію:
#   ./docs/api/generate.sh
set -euo pipefail

REDOCLY_IMAGE="redocly/cli:1.34.5"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/docs/api"
cd "$ROOT"

redocly() {
  docker run --rm -u "$(id -u):$(id -g)" -v "$ROOT":/spec -w /spec "$REDOCLY_IMAGE" "$@"
}

echo "==> 1/3 Валідація spec/api/openapi.yaml"
redocly lint spec/api/openapi.yaml

echo "==> 2/3 REST API: docs/api/rest/index.html"
mkdir -p "$OUT/rest"
redocly build-docs spec/api/openapi.yaml \
  --output docs/api/rest/index.html \
  --title "Vet Clinic API — довідник REST API"

echo "==> 3/3 Javadoc: docs/api/javadoc/"
./mvnw -q javadoc:javadoc
rm -rf "$OUT/javadoc"
cp -r target/reports/apidocs "$OUT/javadoc"

echo "Готово: $OUT/index.md"
