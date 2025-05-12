# Trip Planner Bot (Team №8)
- Бот: https://t.me/TripPlannerBot_bot !!!
- @TripPlannerBot_bot !!!

Telegram бот для планирования путешествий с административной панелью.
## API Endpoints

### Административные эндпоинты

- /admin/users
   - Требует авторизацию
        - Login: AyzekPetyaMartin
        - Password: Ayzek123321    
- /healthceck
   - Проверяет состояние Mongo запросом
   - Выводит ОК - если все хорошо
   - Выводит авторов
## Запуск проекта

### Локальная сборка

1. Сборка проекта с помощью Gradle:
```bash
./gradlew clean build
```

2. Создание исполняемого JAR-файла:
```bash
./gradlew clean shadowJar
```
3. Запуск с Gradle:
``` bash
./gradlew run
```

### Запуск в Docker

Проект использует Docker Compose для оркестрации контейнеров:

1. **docker-compose.yml**:
   - Настраивает сеть `trip-planner-network`
   - Настраивает переменные окружения для приложения
   - Устанавливает зависимости между сервисами
   - Настраивает healthcheck для проверки работоспособности

2. **Dockerfile**:
   - Использует Amazon Corretto 21 (Alpine)
   - Копирует собранный JAR-файл
   - Настраивает точку входа для запуска приложения

### Команды для работы с Docker
1. Сборка:
```bash
docker compose --build -d
docker compose run app
```

Очистка и пересборка:
```bash
# Остановка и удаление контейнеров
docker compose down --remove-orphans

# Удаление всех образов
docker compose down --rmi all

# Сборка и запуск в фоновом режиме
docker compose up --build -d

# Запуск приложения
docker compose run app
```

## Авторы
- Салимли Айзек: 
   - Planner 
   - Docker 
   - gradle
- Григорьев Петр:
   - Helper 
   - Healthcheck
- Михалец Мартин:
   - History 
   - Admin

Проект разработан командой 8 в рамках курса по Java-разработке.
