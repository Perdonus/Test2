# R34 Native

Нативный Android-клиент для `rule34.xxx` на `Kotlin + Jetpack Compose`.

## Что умеет

- Поиск постов по тегам через `api.rule34.xxx`
- Поддержка фото и видео постов
- Быстрый AI-фильтр для скрытия `ai_generated` и `ai_assisted`
- Локальное избранное для постов
- Локальные закладки для поисковых запросов
- Экран деталей с тегами, рейтингом, размерами и быстрым добавлением в избранное
- Настройки `HTTP` и `SOCKS` proxy с логином и паролем
- Сборка debug APK через GitHub Actions

## Важно

У `api.rule34.xxx` сейчас нужен `user_id` и `api_key`. Их можно получить в настройках аккаунта:

`https://rule34.xxx/index.php?page=account&s=options`

Без этих значений приложение не сможет загрузить поиск.

## Локальная сборка

```bash
ANDROID_SDK_ROOT=/usr/lib/android-sdk ./gradlew testDebugUnitTest assembleDebug
```

Если Android SDK ещё пустой, сначала установите пакеты:

```bash
yes | ANDROID_SDK_ROOT=/usr/lib/android-sdk sdkmanager \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.1"
```

## CI

Workflow в `.github/workflows/android.yml` собирает `debug APK` и публикует артефакт.
