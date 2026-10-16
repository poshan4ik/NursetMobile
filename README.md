# NursetMobile — Курсовая работа

## Описание
Android-приложение «Информационная система учёта товаров и заказов» для небольшого магазина.
Реализовано на Kotlin + Jetpack Compose + Room (SQLite) + MVVM.

---

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
│       │   │   │   ├── navigation/              # NursetDestinations, NursetNavHost (BottomNavigation)
│       │   │   │   ├── home/                    # HomeScreen (статистика)
│       │   │   │   ├── products/                # ProductsScreen, ProductFormScreen
│       │   │   │   ├── categories/              # CategoriesScreen
│       │   │   │   └── orders/                  # OrdersScreen, CreateOrderScreen, OrderDetailsScreen
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
- ✅ **Categories**: id, name (уникальное, case-insensitive)
- ✅ **Products**: id, name, categoryId (FK), price (Long, тенге), stock (Int)
- ✅ **Orders**: id, createdAt (Long, ms), totalPrice (Long), status (Int enum)
- ✅ **OrderItems**: id, orderId (FK→orders CASCADE), productId (без FK), productName, price (snapshot), quantity
- ✅ Демоданные при первом запуске: 4 категории, 9 товаров, 4 заказа с разными статусами

### Бизнес-логика
- ✅ **Категории**: CRUD, запрет удаления если есть товары, проверка дубликатов
- ✅ **Товары**: CRUD, поиск по названию, фильтр по категории, низкий остаток (stock ≤ 3), запрет удаления из активных заказов
- ✅ **Заказы**: создание из корзины (транзакция: проверка остатков → заказ → списание), повторное добавление = увеличение количества
- ✅ **Статусы**: NEW→PROCESSING, PROCESSING→COMPLETED, NEW→CANCELLED, PROCESSING→CANCELLED; COMPLETED/CANCELLED — заморожены
- ✅ **Отмена**: транзакция с проверкой статуса из БД, возврат товаров на склад, защита от повторной отмены
- ✅ **Цены**: только Long (тенге), snapshot в позициях заказа

### UI (Jetpack Compose + Material 3)
- ✅ **Главный экран**: статистика (товаров, категорий, заказов, низкий остаток) + быстрая навигация
- ✅ **Нижняя навигация**: 4 вкладки (Главная, Товары, Категории, Заказы)
- ✅ **Товары**: список карточек, поиск в Toolbar, фильтр по категории (Dropdown), индикаторы низкого остатка (⚠), FAB → диалог добавления/редактирования с валидацией
- ✅ **Категории**: список, диалог добавления/редактирования, подтверждение удаления
- ✅ **Создание заказа**: выбор товаров (диалог с поиском/фильтром), кнопки ±, итог в реальном времени, создание заказа
- ✅ **Список заказов**: номер #001, дата, сумма, статус цветом (Chip)
- ✅ **Детали заказа**: позиции (snapshot), итог, кнопки по статусу, диалог подтверждения отмены
- ✅ **Пустые состояния**, Snackbar для ошибок/успеха, Material 3, поддержка тёмной темы

### Тесты (JUnit)
- ✅ **domain/OrderStatusTest**: все допустимые/запрещённые переходы, isActive, fromValue, displayName
- ✅ **domain/BusinessRulesTest**: расчёт стоимости (1000×3=3000), недостаток товара, списание/возврат остатков, защита от повторной отмены
- ✅ **data/repository/CategoryRepositoryTest**: добавление/обновление/удаление, дубликаты, запрет удаления с товарами
- ✅ **data/repository/ProductRepositoryTest**: валидация, дубликаты в категории, поиск
- ✅ **data/repository/OrderRepositoryTest**: создание заказа (успех/нехватка/пустая корзина), отмена NEW/PROCESSING с возвратом, запрет отмены COMPLETED/CANCELLED, защита от повторной отмены, недопустимые переходы статусов

---

## Запуск

### Требования
- Android Studio (встроенный JDK 17)
- minSdk 26, targetSdk 33
- Java 11+ для Gradle (в Android Studio работает из коробки)

### Инструкция
1. Открыть папку `NursetMobile` в Android Studio
2. Дождаться синхронизации Gradle
3. Запустить на эмуляторе (API 26+) или устройстве

### Для подключения к локальному backend (если нужен)
- Android-эмулятор обращается к `10.0.2.2:8000` вместо `localhost`
- Backend должен быть запущен на `0.0.0.0:8000`

---

## Архитектура
```
UI (Compose) → ViewModel → Repository → DAO → Room → SQLite
```
- Слои разделены: `data`, `domain`, `ui`, `viewmodel`
- Зависимости направлены внутрь (UI не знает о Room)
- Repository инкапсулирует бизнес-правила
- ViewModel управляет состоянием UI через Flow/StateFlow

---

## Ключевые решения
1. **OrderItem без FK на Product** — позволяет удалять товары из каталога, сохраняя историю заказов
2. **Snapshot цен/названий** в позициях заказа — история не меняется при изменении каталога
3. **Транзакции Room** для атомарности создания/отмены заказов
4. **Чистые функции в domain** — `canTransitionTo()`, `isActive()` тестируются обычным JUnit без Android
5. **In-memory Room** в тестах репозиториев — быстрые изолированные интеграционные тесты
6. **Type-safe навигация** через sealed interface `NursetDestination`