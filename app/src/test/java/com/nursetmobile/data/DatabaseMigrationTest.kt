package com.nursetmobile.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nursetmobile.data.entity.ClientEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {
    @Test fun `обновление с версии 1 сохраняет существующие данные`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
            db.execSQL("CREATE TABLE products (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, categoryId INTEGER NOT NULL, price INTEGER NOT NULL, stock INTEGER NOT NULL, FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE NO ACTION)")
            db.execSQL("CREATE INDEX index_products_categoryId ON products(categoryId)")
            db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, createdAt INTEGER NOT NULL, totalPrice INTEGER NOT NULL, status INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE order_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, orderId INTEGER NOT NULL, productId INTEGER NOT NULL, productName TEXT NOT NULL, price INTEGER NOT NULL, quantity INTEGER NOT NULL, FOREIGN KEY(orderId) REFERENCES orders(id) ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX index_order_items_orderId ON order_items(orderId)")
            db.execSQL("CREATE INDEX index_order_items_productId ON order_items(productId)")
            db.execSQL("INSERT INTO categories VALUES (1, 'Техника')")
            db.execSQL("INSERT INTO products VALUES (1, 'Телефон', 1, 1000, 5)")
            db.execSQL("INSERT INTO orders VALUES (1, 100, 2000, 0)")
            db.execSQL("INSERT INTO order_items VALUES (1, 1, 1, 'Телефон', 1000, 2)")
            db.version = 1
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val order = database.orderDao().getOrderById(1)!!
            assertEquals(2000L, order.totalPrice)
            assertNull(order.clientId)
            assertEquals("", order.clientName)
            assertEquals(1, database.orderDao().getItemsByOrderId(1).size)
            assertEquals(5, database.productDao().getById(1)?.stock)
            assertNotNull(database.categoryDao().getById(1))
            val id = database.clientDao().insert(ClientEntity(name = "Анна", phone = "79991234567"))
            assertEquals("Анна", database.clientDao().getById(id)?.name)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
