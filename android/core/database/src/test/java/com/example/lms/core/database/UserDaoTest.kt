package com.example.lms.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.lms.core.database.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserDaoTest {
    private lateinit var database: LmsDatabase

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LmsDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `observeUser emits inserted user`() = runBlocking {
        val user = UserEntity("1", "Test", "test@example.com", "LEARNER", "org")
        database.userDao().upsertAll(listOf(user))
        val loaded = database.userDao().observeUser("1").first()
        assertEquals(user, loaded)
    }
}

