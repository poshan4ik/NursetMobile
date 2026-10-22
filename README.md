# NursetMobile — Курсовая работа

## Описание
Android-приложение «Информационная система учёта товаров и заказов» для небольшого магазина.
Реализовано на Kotlin + Jetpack Compose + Room (SQLite) + MVVM.

## Структура проекта

```
NursetMobile/
├── app/
│   ├── build.gradle.kts          # Зависимости: Compose, Material3, Navigation, Room, KAPT, Coroutines
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/nursetmobile/
│       │   │   ├── MainActivity.kt              # Точка входа, тема, навигация
│       │   │   ├── NursetApplication.kt         # Инициализация Room Database
│       │   │   ├── data/
│       │   │   │   ├── dao/                     # CategoryDao, ProductDao, OrderDao
│       │   │   │   ├── entity/                  # CategoryEntity, ProductEntity, OrderEntity, OrderItemEntity
│       │   │   │   ├── repository/              # CategoryRepository, ProductRepository, OrderRepository
│       │   │   │   └── AppDatabase.kt           # RoomDatabase + демоданные
│       │   │   ├── domain/
│       │   │   │   └── OrderStatus.kt           # Enum + canTransitionTo() + isActive()
│       │   │   ├── ui/
│       │   │   │   ├── navigation/              # NursetNavHost (нижняя навигация)
│       │   │   │   ├── home/                    # HomeScreen (статистика)
│       │   │   │   ├── products/                # ProductsScreen (+ диалог добавления/редактирования)
│       │   │   │   ├── categories/              # CategoriesScreen
│       │   │   │   ├── orders/                  # OrdersScreen, CreateOrderScreen, OrderDetailsScreen
│       │   │   │   └── utils.kt                 # formatPrice(), StatusChip
│       │   │   └── viewmodel/                   # HomeViewModel, CategoryViewModel, ProductViewModel, OrderViewModel
│       │   └── res/
│       │       ├── values/strings.xml, colors.xml, themes.xml
│       │       └── values-night/themes.xml      # Тёмная тема
│       └── test/
│           └── java/com/nursetmobile/
│               ├── domain/                      # OrderStatusTest, BusinessRulesTest
│               └── data/repository/             # CategoryRepositoryTest, ProductRepositoryTest, OrderRepositoryTest
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew / gradlew.bat
└── .gitignore
```

---

## Реализованная функциональность (по ТЗ)

### База данных (Room)
- **Categories**: id, name (уникальное, case-insensitive)
- **Products**: id, name, categoryId (FK), price (Long, тенге), stock (Int)
- **Orders**: id, createdAt (Long, ms), totalPrice (Long), status (Int enum)
- **OrderItems**: id, orderId (FK→orders CASCADE), productId (без FK), productName, price (snapshot), quantity
- Демоданные при первом запуске: 4 категории, 9 товаров, 4 заказа с разными статусами

### Бизнес-логика
- **Категории**: CRUD, запрет удаления если есть товары, проверка дубликатов
- **Товары**: CRUD, поиск по названию, фильтр по категории, низкий остаток (stock ≤ 3), запрет удаления из активных заказов
- **Заказы**: создание из корзины в одной транзакции Room (проверка остатков → заказ → списание), повторное добавление товара = объединение количеств
- **Статусы**: NEW→PROCESSING, PROCESSING→COMPLETED, NEW→CANCELLED, PROCESSING→CANCELLED; COMPLETED/CANCELLED — заморожены
- **Отмена**: транзакция с возвратом товаров на склад, защита от повторной отмены
- **Цены**: только Long (тенге), снимок в позициях заказа, сумма заказа считается по ценам из базы

### UI (Jetpack Compose + Material 3)
- **Главный экран**: статистика (товаров, категорий, заказов, низкий остаток) + быстрая навигация
- **Нижняя навигация**: 4 вкладки (Главная, Товары, Категории, Заказы)
- **Товары**: список карточек, поиск, фильтр по категории, индикаторы низкого остатка (⚠), диалог добавления/редактирования с валидацией
- **Категории**: список, диалог добавления/редактирования, подтверждение удаления
- **Создание заказа**: выбор товаров (диалог с поиском), кнопки ±, итог в реальном времени
- **Список заказов**: номер #001, дата, сумма, статус
- **Деталей заказа**: позиции (снимок цен), итог, кнопки по статусу, подтверждение отмены
- Пустые состояния, Snackbar для ошибок/успеха, тёмная тема

### Тесты (JUnit + Robolectric) — всего 74
- **domain/OrderStatusTest**: все допустимые/запрещённые переходы, isActive, fromValue
- **domain/BusinessRulesTest**: расчёт стоимости, недостаток товара, защита от повторной отмены
- **ui/UtilsTest**: форматирование цены (разные локали, разделители)
- **data/repository/CategoryRepositoryTest**: добавление/обновление/удаление, дубликаты, запрет удаления с товарами
- **data/repository/ProductRepositoryTest**: валидация, дубликаты в категории, поиск, фильтр по категории, обновление, малые остатки
- **data/repository/OrderRepositoryTest**: создание заказа (транзакция, остатки, сумма по ценам из БД, объединение дублей), отмена с возвратом товаров, переходы статусов, несуществующие заказы

---

## Запуск

### Требования
- Android Studio (встроенный JDK 17)
- minSdk 26, targetSdk 34

### Приложение
1. Открыть папку `NursetMobile` в Android Studio
2. Дождаться синхронизации Gradle
3. Нажать Run ▶ и выбрать эмулятор (API 26+) или устройство

Или из командной строки:

```
.\gradlew.bat installDebug
```

### Тесты

```
.\gradlew.bat test
```

Или в Android Studio: правый клик по папке `app/src/test` → Run Tests.

---

## Архитектура
```
UI (Compose) → ViewModel → Repository → DAO → Room → SQLite
```
- Слои разделены: `data`, `domain`, `ui`, `viewmodel`
- Repository инкапсулирует бизнес-правила
- ViewModel управляет состоянием UI через Flow/StateFlow

---

## Ключевые решения
1. **OrderItem без FK на Product** — позволяет удалять товары из каталога, сохраняя историю заказов
2. **Снимок цен/названий** в позициях заказа — история не меняется при изменении каталога
3. **Транзакции Room** (`withTransaction`) для атомарности создания/отмены заказов
4. **Чистые функции в domain** — `canTransitionTo()`, `isActive()` тестируются обычным JUnit без Android
5. **In-memory Room** в тестах репозиториев — быстрые изолированные интеграционные тесты
