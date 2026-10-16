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
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = newInstance
                    newInstance
                }
            }
        }

        // Заполняет базу демоданными при первом запуске (категории, товары, заказы)
        suspend fun populateDemoDataIfEmpty(db: AppDatabase) {
            val categoriesCount = db.categoryDao().getAll().first().size
            val ordersCount = db.orderDao().countOrders()
            if (categoriesCount > 0 || ordersCount > 0) return

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
            products.forEach { db.productDao().insert(it) }

            // Заказы с разными статусами; ID товаров указаны по порядку их добавления
            val order1 = OrderEntity(System.currentTimeMillis() - 86400000 * 5, 0, 0)
            val order1Id = db.orderDao().insertOrder(order1)
            val items1 = listOf(
                OrderItemEntity(order1Id, 1, "Samsung Galaxy S25", 350000, 2),
                OrderItemEntity(order1Id, 4, "AirPods 4", 90000, 1)
            )
            db.orderDao().insertOrderItems(items1)
            val total1 = items1.sumOf { it.price * it.quantity }
            db.orderDao().updateOrder(order1.copy(id = order1Id, totalPrice = total1))

            val order2 = OrderEntity(System.currentTimeMillis() - 86400000 * 3, 0, 1)
            val order2Id = db.orderDao().insertOrder(order2)
            val items2 = listOf(
                OrderItemEntity(order2Id, 2, "Redmi Note 14", 120000, 1),
                OrderItemEntity(order2Id, 5, "Power Bank", 25000, 2)
            )
            db.orderDao().insertOrderItems(items2)
            val total2 = items2.sumOf { it.price * it.quantity }
            db.orderDao().updateOrder(order2.copy(id = order2Id, totalPrice = total2))

            val order3 = OrderEntity(System.currentTimeMillis() - 86400000 * 10, 0, 2)
            val order3Id = db.orderDao().insertOrder(order3)
            val items3 = listOf(
                OrderItemEntity(order3Id, 3, "iPhone 16", 450000, 1)
            )
            db.orderDao().insertOrderItems(items3)
            val total3 = items3.sumOf { it.price * it.quantity }
            db.orderDao().updateOrder(order3.copy(id = order3Id, totalPrice = total3))

            val order4 = OrderEntity(System.currentTimeMillis() - 86400000 * 7, 0, 3)
            val order4Id = db.orderDao().insertOrder(order4)
            val items4 = listOf(
                OrderItemEntity(order4Id, 6, "MacBook Air M3", 800000, 1)
            )
            db.orderDao().insertOrderItems(items4)
            val total4 = items4.sumOf { it.price * it.quantity }
            db.orderDao().updateOrder(order4.copy(id = order4Id, totalPrice = total4))
        }
    }
}
