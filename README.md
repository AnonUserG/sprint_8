# Online store

Витрина интернет-магазина на основе Spring Boot, Redis, reactive stack, payment and security service.

### 🚀 Как запустить проект на Windows (cmd)

#### 📦 Требования
- Git
- Docker
- Порты 8075, 9090, 5433, 8080, 8081, 6379 - должны быть свободны


#### 📁 Запуск

1. Клонируй ветку main из репозитория используя git bash
   ```bash
   git clone --branch main --single-branch https://github.com/AnonUserG/sprint_8.git
2. Запусти на машине Docker
3. Перейди в папку с проектом
4. Подними проект (тесты прогонятся автоматически)
   ```bash
   docker compose up --build
5. Дождись запуска всех контейнеров

6. Перейди в браузере на [http://localhost:8075/](http://localhost:8075/)

7. Доступно 2 пользователя для авторизации
   admin:password и customer:password