package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.ModuleDao
import com.example.data.db.ModuleEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var moduleDao: ModuleDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        moduleDao = db.moduleDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ZX Shell Runner", appName)
    }

    @Test
    fun insertAndRetrieveModule() = runBlocking {
        val module = ModuleEntity(
            name = "test_script.sh",
            uri = "content://com.android.providers.downloads.documents/document/123",
            size = 1024L,
            importDate = System.currentTimeMillis(),
            validationStatus = "VALID",
            validationMessage = "Valid shell script",
            previewContent = "#!/system/bin/sh\necho Hello"
        )

        val id = moduleDao.insertModule(module)
        val retrieved = moduleDao.getModuleById(id)

        assertNotNull(retrieved)
        assertEquals("test_script.sh", retrieved?.name)
        assertEquals("VALID", retrieved?.validationStatus)

        val allModules = moduleDao.getAllModules().first()
        assertEquals(1, allModules.size)

        // Test delete
        retrieved?.let { moduleDao.deleteModule(it) }
        val afterDelete = moduleDao.getModuleById(id)
        assertNull(afterDelete)
    }
}
