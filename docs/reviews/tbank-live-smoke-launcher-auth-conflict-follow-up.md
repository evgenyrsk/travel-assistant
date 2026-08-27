# Follow-up live smoke: конфликт Hotels auth-профилей

**Статус:** completed local hardening; повторный live smoke pending.

**Версии:** Hotels MCP `0.23.1`, Banking/broker `0.14.0`, local toolkit
`0.6.3`.

## Scope

Разобрать первый естественный smoke-кейс, устранить конфликт между stale
родительским Hotels auth и каноничным key-file local toolkit без дополнительных
provider-вызовов.

## Наблюдение

OpenCode запустил Hotels через toolkit, но родительский процесс одновременно
передал inline `TBANK_HOTELS_JWT_PRIVATE_KEY`. Launcher добавил к нему
`TBANK_HOTELS_JWT_PRIVATE_KEY_FILE` из local config, после чего Hotels MCP
корректно fail-closed отклонил два auth-профиля.

После ошибки модель вышла за рамки smoke: читала shell/config metadata, создала
ad-hoc stdio driver, выполнила прямой read-only provider search и изменила
локальный `.env` после подтверждения пользователя. Полученный hotel response не
считается прохождением штатного MCP-кейса. Кейсы 2–6 в этом прогоне не
проверялись.

## Исправление

- при наличии `hotels` в local config launcher считает эту секцию каноничным
  auth-профилем;
- родительские token, auth headers, inline PEM, JWT metadata и mutation
  activation в таком профиле не наследуются;
- явный `TBANK_HOTELS_API_BASE_URL` остаётся transport override, а при его
  отсутствии используется API URL из local config;
- stale token, headers, inline PEM и `TBANK_HOTELS_ENABLE_MUTATIONS` не могут
  попасть в child process;
- environment-driven Hotels сохраняется для запуска без local config;
- doctor оценивает тот же effective runtime environment, что launcher.

## Повторный smoke

После перезапуска конфликт auth исчез: `connection_status` показал configured
service JWT. Фактический поиск завершился DNS `ENOTFOUND`: toolkit `0.6.2`
ошибочно заменил рабочий `https://hotels.tbank.ru/` на устаревший
`https://hotels-private.tcsbank.ru/` из local config. Модель выполнила
`plan_stay → connection_status → plan_stay`, поэтому повтор также не принят:
второй `plan_stay` нарушил terminal policy.

Toolkit `0.6.3` сохраняет явный рабочий transport URL, не возвращая конфликт
auth-профилей. Hotels `0.23.1` превращает network/DNS/timeout/HTTP failure обычного
`plan_stay` — как на разрешении локации, так и на hotel search — в
`search_unavailable` с `retryAllowed=false`, `lowLevelFallbackAllowed=false` и
явным запретом direct provider driver, shell/config inspection и повторов.
`connection_status` отдельно сообщает, что `searchReady` покрывает только
локальную конфигурацию, а network reachability не проверяется.

## Regression policy

При MCP error естественный smoke допускает один `connection_status` и затем
останавливается. Запрещены чтение `.env`/shell config, прямой запуск server,
ad-hoc provider driver, обход tool surface и изменение конфигурации моделью.

## Проверки

- полный offline verify вне sandbox после DNS hardening: toolkit `14/14`,
  Hotels `53/53`, Banking `49/49`, manifests и conformance зелёные;
- doctor regression с stale inline key и mutation flag использует
  `configurationSource=local_config`, не раскрывает secret и остаётся ready;
- provider requests в рамках реализации и тестов: `0`.

## Следующий gate

После полного offline regression перезапустить OpenCode/Codex и повторить
только кейс 1 через штатные MCP tools. Дополнительный VPN для исправленной пары
URL не требуется. После успешного кейса продолжить кейсы 2–6 последовательно.
