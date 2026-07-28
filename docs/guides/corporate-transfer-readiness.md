# Готовность переноса в корпоративную инфраструктуру

## Назначение

Checklist фиксирует минимальный безопасный путь переноса Travel Assistant с
полной историей и без преждевременной активации внешних моделей. Текущий backend
остаётся Kotlin + Ktor на Java 17; semantic runtime по умолчанию — `FAKE`.

## 1. Перенос Git

- [ ] Создать корпоративный repository с запрещённым force-push для `main` и
      `semantic-analysis`.
- [ ] Перенести полную commit history, все необходимые branches и tags.
- [ ] Экспортировать PR, reviews, issues и discussion metadata отдельно: эти
      данные не входят в Git history.
- [ ] Сохранить `main` без semantic functionality; semantic history продолжить
      от `semantic-analysis`.
- [ ] Выполнить secret scan всей истории до первого push; найденные credentials
      отозвать, даже если они удалены из текущего tree.
- [ ] Проверить dependency licenses и сформировать корпоративный SBOM штатным
      инструментом целевой платформы.

## 2. Воспроизводимая сборка

- [ ] Предоставить Java 17 и Node.js, совместимые с repository checks.
- [ ] Разрешить Gradle wrapper и npm dependencies через корпоративные mirrors.
- [ ] Перенести обязательные backend, frontend, launcher, OpenAPI и semantic
      evaluation gates в CI без REAL provider/model calls.
- [ ] Хранить secrets только в approved secret manager; не помещать их в Git,
      images, command line, logs или metrics.

## 3. Корпоративный semantic gateway

- [ ] Реализовать contract v1 из
      [технической спецификации](../architecture/stage-17/internal-semantic-gateway-contract-v1.md)
      и границу из
      [`ADR-0002`](../decisions/adr-0002-provider-neutral-semantic-gateway-boundary.md).
- [ ] Зафиксировать точный HTTPS endpoint, workload identity/auth scheme,
      network policy и TLS ownership.
- [ ] Зафиксировать opaque deployment IDs и запретить незаметный routing на
      другой model/provider.
- [ ] Обеспечить доступ gateway только к разрешённым image hosts либо заменить
      URL на одобренный внутренний object-reference transport отдельной версией
      contract.
- [ ] Подтвердить retention, logging, training, data region, dataset location,
      access и deletion rules.

До закрытия checklist `ACCOMMODATION_ANALYSIS_MODE` остаётся `FAKE`. Наличие
adapter само по себе не является разрешением передавать provider content.

## 4. Выбор модели или моделей

Выбирать нужно deployment, а не marketing model name:

`model version + inference runtime + hardware + quantization + gateway settings`.

Первый shortlist содержит 2–3 реально доступных внутри корпоративного контура
deployment:

1. быстрый/cost baseline;
2. основной balanced candidate;
3. quality candidate, если платформа позволяет.

Каждый candidate обязан поддерживать русский текст, минимум три изображения,
strict structured response, batch не менее шести deep candidates, pinned
version, допустимую лицензию и отсутствие запрещённого egress.

Все deployments проверяются на одном rights-approved dataset: минимум 100
candidates из трёх направлений, включая обычные отели, подтверждённые glamping
и borderline subset с двумя независимыми reviewer. Обязательные thresholds:

- `MATCH precision >= 90%`;
- `MATCH + PROBABLE precision >= 80%`;
- recall `>= 70%`;
- false-positive rate обычных отелей `<= 5%`.

Сначала исключаются deployment, не прошедшие качество. Среди прошедших
выбирается минимальная измеренная стоимость, затем меньшая latency. Сначала один
deployment обслуживает coarse и deep passes. Второй прошедший deployment может
быть ручным rollback target, но не automatic fallback.

## 5. Порядок активации

1. Network-free contract tests gateway.
2. Один approved probe выбранного deployment без retry/fallback.
3. Offline evaluation общего dataset.
4. Shadow run без влияния на ranking и пользовательский результат.
5. Bounded canary с готовым переключением обратно на `FAKE`.
6. Отдельное rollout decision после quality, privacy и operations review.

## Открытые внешние входные данные

До переноса кода не требуется выбирать их предположением:

- корпоративная AI/GPU platform;
- доступные и лицензированные multimodal models;
- identity mechanism и secret delivery;
- разрешённый image transport;
- dataset storage и data governance;
- SLO, capacity и стоимость inference.

Эти пункты определяют concrete deployment, но не требуют изменений domain,
application orchestration или public API Travel Assistant.
