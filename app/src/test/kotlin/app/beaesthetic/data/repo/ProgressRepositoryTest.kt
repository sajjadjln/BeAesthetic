package app.beaesthetic.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.beaesthetic.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ProgressRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var repo: ProgressRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = ProgressRepository(db, PhotoStore(context))
    }

    @After
    fun tearDown() = db.close()

    private fun titles() = runBlocking { db.progress().observeMilestones().first().map { it.title } }

    @Test
    fun `editing a milestone keeps its place, a new one goes last`() = runBlocking {
        repo.saveMilestone(null, "a", null, null, null)
        repo.saveMilestone(null, "b", null, null, null)
        repo.saveMilestone(null, "c", null, null, null)
        val b = db.progress().observeMilestones().first().first { it.title == "b" }

        repo.saveMilestone(b.id, "b2", null, null, LocalDate.of(2026, 11, 1))
        assertEquals(listOf("a", "b2", "c"), titles())

        repo.saveMilestone(null, "d", null, null, null)
        assertEquals(listOf("a", "b2", "c", "d"), titles())
    }
}
