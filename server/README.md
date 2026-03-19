# Rule Server

Единый backend для Android-клиента. Хранит общие для всех пользователей:

- избранные посты
- сохранённые поисковые запросы
- серверный прокси

Также сервер делает AI-нормализацию поискового запроса через `sosiskibot.ru`, а затем валидирует booru-теги по API `rule34`, `konachan` и `xbooru`.

## Локальный запуск

```bash
cd /root/r34
python3 server/rule_server.py
```

Основные env-переменные:

- `RULE_SERVER_HOST`
- `RULE_SERVER_PORT`
- `RULE_PUBLIC_BASE_URL`
- `RULE_SERVER_DATA_ROOT`
- `RULE_AI_BASE_URL`
- `RULE_AI_API_KEY`
- `RULE_AI_MODEL`
- `RULE34_USER_ID`
- `RULE34_API_KEY`

По умолчанию service слушает `127.0.0.1:5006`, а публичная точка находится на `https://sosiskibot.ru/rule`.

## API

- `GET /api/health`
- `GET /api/state`
- `GET /api/proxy`
- `POST /api/favorites/toggle`
- `POST /api/saved-searches`
- `POST /api/saved-searches/rename`
- `POST /api/saved-searches/delete`
- `POST /api/proxy`
- `POST /api/resolve-query`

## Deployment

- systemd unit: [r34-rule-server.service](/root/r34/server/r34-rule-server.service)
- nginx проксирует `/rule/` на `127.0.0.1:5006`

## Проверка

```bash
curl --noproxy '*' https://sosiskibot.ru/rule/api/health

curl --noproxy '*' -X POST https://sosiskibot.ru/rule/api/resolve-query \
  -H 'Content-Type: application/json' \
  --data '{"serviceId":"rule34","query":"Ручими Курикими","mode":"ai"}'
```
