# ZChat - Мессенджер

Нативный Android мессенджер с Firebase.

## Быстрый старт

### 1. Создайте репозиторий на GitHub

### 2. Загрузите все файлы из этого архива

### 3. Добавьте google-services.json

**ВАЖНО:** Скопируйте ваш `google-services.json` в папку `app/`

Структура должна быть:
```
app/
├── google-services.json  <-- СЮДА!
├── build.gradle
├── src/
│   └── main/
│       ├── AndroidManifest.xml
│       ├── java/
│       └── res/
├── build.gradle
├── settings.gradle
└── .github/
    └── workflows/
        └── build-apk.yml
```

### 4. Запустите сборку

1. GitHub → Actions → "Build ZChat APK" → Run workflow
2. Скачайте APK из Artifacts

## Firebase настройки

В Firebase Console должны быть включены:

1. **Authentication** → Email/Password → **Включить**
2. **Realtime Database** → Создать базу → Тестовый режим

## Функции

- ✅ Регистрация и вход
- ✅ Список пользователей
- ✅ Чаты
- ✅ Настройки
- ✅ Тёмная тема

## Требования

- Android 7.0+ (API 24)
- Интернет соединение
