# Stage 16.1 — Semantic accommodation conversation model

## Статус

Завершён. Следующий разрешённый шаг — Stage 16.2.

## Scope

- [x] Добавлен закрытый domain concept `AccommodationConcept.GLAMPING`.
- [x] Preference сохраняется в session-local constraints между уточнениями.
- [x] Поддержаны явные set и clear для русских вариантов и `glamping`.
- [x] Неактивные категории, включая apartments, не преобразуются в concept.
- [x] Strict text-LLM schema расширена только managed значением `glamping`.
- [x] Concept включён в confirmation summary/display fields и idempotency key.
- [x] Запрос booking получает явное сообщение, что сервис выполняет подбор, а
  не бронирование.
- [x] Снятие preference во время активного confirmation распознаётся как
  correction и запускает безопасное replanning.

## Out of scope

- semantic classification и verdict types;
- provider search wiring;
- async search lifecycle;
- details/image analysis;
- OpenRouter vision adapter и REAL calls;
- public API/OpenAPI/frontend changes.

## Architecture review

- `AccommodationConcept` находится в domain и не зависит от Ktor/provider/LLM.
- Conversation enrichment является application policy с закрытой taxonomy.
- Текстовый `LlmClient` только извлекает managed preference и не получает
  multimodal responsibility.
- Infrastructure schema не принимает свободные accommodation labels.
- Provider mapping, ranking и ordinary search behavior на этом sub-stage не
  изменены.

## Проверки

- [x] Parser tests: set, clear, English alias и inactive category.
- [x] Conversation integration: booking boundary, persistence, confirmation и
  clear.
- [x] Preference mapping/application tests.
- [x] Confirmation и idempotency tests.
- [x] Strict OpenRouter text contract tests.
- [x] Backend `./gradlew test` — passed.
- [x] `git diff --check` — gate commit.

## Findings

Во время integration test обнаружено, что фразы «убери» и «сними» при активном
confirmation классифицировались как unknown и не доходили до replanning.
Correction classifier расширен этими явными markers; существующие positive,
negative и ambiguous replies не изменены.

## Итог

Conversation state полностью готов передавать managed concept в будущий
semantic search. До Stage 16.4 подтверждение semantic request не должно
считаться готовностью async execution; этот runtime wiring остаётся отдельным
commit.
