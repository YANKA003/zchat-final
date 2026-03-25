# ZChat Cloud Functions

## Описание

Cloud Functions для мессенджера ZChat. Автоматически отправляют push-уведомления о новых сообщениях и звонках.

## Функции

| Функция | Описание |
|---------|----------|
| `onMessageCreated` | Отправляет уведомление о новом сообщении |
| `onCallCreated` | Отправляет уведомление о входящем звонке |
| `onCallStatusChanged` | Очищает данные после завершения звонка |
| `onUserDisconnect` | Обновляет время последнего посещения |
| `checkPremiumStatus` | Проверяет истечение Premium подписки |
| `cleanOldNotifications` | Удаляет старые уведомления (ежедневно) |

---

## Установка

### 1. Установите Node.js
Скачайте с https://nodejs.org/ (версия 18 или выше)

### 2. Установите Firebase CLI
```bash
npm install -g firebase-tools
```

### 3. Войдите в Firebase
```bash
firebase login
```

### 4. Перейдите в папку functions и установите зависимости
```bash
cd functions
npm install
```

---

## Деплой

### Первый раз (инициализация)
```bash
# В корне проекта
firebase init functions
# Выберите ваш проект zchat-6b877
# Выберите TypeScript
# Выберите ESLint (опционально)
```

### Деплой функций
```bash
# Из корня проекта
firebase deploy --only functions
```

### Деплой только одной функции
```bash
firebase deploy --only functions:onMessageCreated
```

---

## Локальное тестирование

### Запуск эмулятора
```bash
cd functions
npm run serve
```

### Просмотр логов
```bash
firebase functions:log
```

---

## Важные требования

### План Blaze
Для работы Cloud Functions нужен план **Blaze (Pay as you go)**.

⚠️ **Но это почти бесплатно!**
- 125,000 вызовов/день бесплатно
- 2 млн вызовов/месяц = $0.40
- Для личного проекта это копейки

### Как включить:
1. Firebase Console → Your Project
2. ⚙️ Settings → Usage and billing
3. Details → Modify plan → Blaze

---

## Структура файлов

```
zchat-app/
├── firebase.json          # Конфигурация Firebase
├── database.rules.json    # Правила безопасности
└── functions/
    ├── package.json       # Зависимости Node.js
    ├── tsconfig.json      # Настройки TypeScript
    ├── .gitignore
    └── src/
        └── index.ts       # Код функций
```

---

## Диагностика проблем

### Функция не работает
1. Проверьте логи: `firebase functions:log`
2. Проверьте, что пользователь имеет FCM token
3. Проверьте, что уведомления включены на устройстве

### Ошибка "Missing FCM token"
Пользователь должен войти в приложение хотя бы один раз после установки функций.

### Уведомления не приходят
1. Проверьте разрешение на уведомления в настройках Android
2. Проверьте, что приложение не в "режиме экономии энергии"
3. Проверьте логи функций

---

## Стоимость (оценка)

| Количество пользователей | Вызовов/день | Стоимость/месяц |
|--------------------------|--------------|-----------------|
| 10 пользователей | ~100 | $0 |
| 100 пользователей | ~1,000 | $0 |
| 1,000 пользователей | ~10,000 | $0 |
| 10,000 пользователей | ~100,000 | ~$1 |

---

## Поддержка

Если возникли проблемы:
1. Firebase Console → Functions → Logs
2. Проверьте документацию: https://firebase.google.com/docs/functions
