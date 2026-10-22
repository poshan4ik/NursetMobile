package com.nursetmobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nursetmobile.data.dao.CategoryDao
import com.nursetmobile.data.dao.OrderDao
import com.nursetmobile.data.dao.ProductDao
import com.nursetmobile.data.entity.CategoryEntity
import com.nursetmobile.data.entity.OrderEntity
import com.nursetmobile.data.entity.OrderItemEntity
import com.nursetmobile.data.entity.ProductEntity
import com.nursetmobile.domain.OrderStatus
import kotlinx.coroutines.flow.first

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "nurset_mobile.db"

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE
                if (instance != null) {
                    instance
                } else {
                    val newInstance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME
                    )
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .build()
                    INSTANCE = newInstance
                    newInstance
                }
            }
        }

        // Заполняет базу демоданными при первом запуске (категории, товары, заказы)
        suspend fun populateDemoDataIfEmpty(db: AppDatabase) {
            val hasData = db.categoryDao().getAll().first().isNotEmpty() || db.orderDao().countOrders() > 0
            if (hasData) return

            val catPhonesId = db.categoryDao().insert(CategoryEntity("Смартфоны"))
            val catLaptopsId = db.categoryDao().insert(CategoryEntity("Ноутбуки"))
            val catAccessoriesId = db.categoryDao().insert(CategoryEntity("Аксессуары"))
            val catAppliancesId = db.categoryDao().insert(CategoryEntity("Бытовая техника"))

            val products = listOf(
                ProductEntity("Samsung Galaxy S25", catPhonesId, 350000, 5),
                ProductEntity("Redmi Note 14", catPhonesId, 120000, 10),
                ProductEntity("iPhone 16", catPhonesId, 450000, 3),
                ProductEntity("AirPods 4", catAccessoriesId, 90000, 7),
                ProductEntity("Power Bank", catAccessoriesId, 25000, 12),
                ProductEntity("MacBook Air M3", catLaptopsId, 800000, 2),
                ProductEntity("ASUS ROG Strix", catLaptopsId, 1200000, 1),
                ProductEntity("Чайник Xiaomi", catAppliancesId, 4500, 20),
                ProductEntity("Пылесос робот", catAppliancesId, 35000, 4)
            )
            // Запоминаем реальные ID из базы — никаких предположений о нумерации
            val idByName = products.associate { it.name to db.productDao().insert(it) }

            // (дней назад, статус, позиции: название товара to количество)
            val demoOrders = listOf(
                Triple(5, OrderStatus.NEW, listOf("Samsung Galaxy S25" to 2, "AirPods 4" to 1)),
                Triple(3, OrderStatus.PROCESSING, listOf("Redmi Note 14" to 1, "Power Bank" to 2)),
                Triple(10, OrderStatus.COMPLETED, listOf("iPhone 16" to 1)),
                Triple(7, OrderStatus.CANCELLED, listOf("MacBook Air M3" to 1))
            )
            demoOrders.forEach { (daysAgo, status, items) ->
                val positions = items.map { (productName, quantity) ->
                    val product = products.first { it.name == productName }
                    OrderItemEntity(
                        orderId = 0,
                        productId = idByName.getValue(productName),
                        productName = product.name,
                        price = product.price,
                        quantity = quantity
                    )
                }
                val totalPrice = positions.sumOf { it.price * it.quantity }
                db.orderDao().createOrderWithItems(
                    OrderEntity(
                        createdAt = System.currentTimeMillis() - daysAgo * 86400000L,
                        totalPrice = totalPrice,
                        status = status.value
                    ),
                    positions
                )
            }
        }
    }
}
