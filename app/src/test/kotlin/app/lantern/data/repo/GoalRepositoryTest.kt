package app.lantern.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.lantern.backup.BackupArchive
import app.lantern.data.db.AppDatabase
import app.lantern.domain.goals.*
import app.lantern.domain.tasks.TaskForms
import app.lantern.domain.time.Weekdays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class GoalRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.of(2026, 11, 17)
    private val now = Instant.parse("2026-11-17T08:00:00Z")
    private lateinit var dir: File
    private lateinit var db: AppDatabase
    private fun open() = Room.databaseBuilder(context, AppDatabase::class.java, File(dir, "goals.db").path).allowMainThreadQueries().build()
    private val tasks get() = TaskRepository(db)
    private val goals get() = GoalRepository(db, tasks)
    private fun draft() = GoalDraft(title = "کتاب بخونم", start = today, finish = today.plusDays(90), triedBefore = false,
        milestones = mapOf(today to "کتاب اول"), actions = listOf(GoalActionDraft("۵ صفحه بخونم", Weekdays.ALL)), why = "برای یاد گرفتن و آرامش خودم")

    @Before fun setUp() = runBlocking {
        dir = File(context.cacheDir, "goal-repository").apply { deleteRecursively(); mkdirs() }
        db = open()
        Seed.ensure(db, today, now)
        Unit
    }
    @After fun tearDown() { db.close(); dir.deleteRecursively() }

    @Test fun `pin survives closing database and reopening and ticks are shared with Today`() = runBlocking {
        val id = goals.pin(draft(), today, now)
        val action = tasks.day(today).items.single().task
        assertEquals(id, action.goalId)
        tasks.toggle(action.id, today, now)
        db.close()
        db = open()
        assertEquals(draft().why, db.goals().get(id)!!.why)
        assertEquals("کتاب اول", db.goals().observeMilestones().first().first().statement)
        assertTrue(tasks.day(today).items.single().done)
        assertEquals(1, db.completions().count(action.id, today))
        val original = tasks.task(action.id)!!
        tasks.saveForm(TaskForms.from(original).copy(title = "۱۰ صفحه بخونم"), original, today, now)
        assertEquals(id, tasks.task(action.id)!!.goalId)
    }

    @Test fun `achieving and setting aside stop tasks but retain history and can resume`() = runBlocking {
        val id = goals.pin(draft(), today, now)
        val action = tasks.day(today).items.single().task
        tasks.toggle(action.id, today, now)
        goals.setStatus(id, GoalStatus.SET_ASIDE, today, now)
        assertTrue(tasks.day(today).items.isEmpty())
        assertEquals(1, db.completions().count(action.id, today))
        goals.setStatus(id, GoalStatus.ACTIVE, today, now)
        assertTrue(tasks.day(today).items.single().done)
        goals.setStatus(id, GoalStatus.ACHIEVED, today, now)
        assertTrue(tasks.day(today).items.isEmpty())
        assertEquals(now, db.goals().get(id)!!.achievedAt)
        assertEquals(1, db.completions().count(action.id, today))
    }

    @Test fun `retrying the same pin does not duplicate goal or Today tasks`() = runBlocking {
        val d = draft()
        val id = goals.pin(d, today, now)
        assertEquals(id, goals.pin(d, today, now))
        assertEquals(1, db.goals().observeAll().first().size)
        assertEquals(1, db.tasks().active().size)
    }

    @Test fun `invalid plan leaves no partial note or tasks`() = runBlocking {
        try { goals.pin(draft().copy(why = " "), today, now); fail("blank why saved") } catch (_: IllegalArgumentException) { }
        assertTrue(db.goals().observeAll().first().isEmpty())
        assertTrue(db.tasks().active().isEmpty())
    }

    @Test fun `backup and restore preserve goals milestones why and linked completions`() = runBlocking {
        val id = goals.pin(draft(), today, now)
        val action = tasks.day(today).items.single().task
        tasks.toggle(action.id, today, now)
        val zip = File(dir, "goal-backup.zip")
        val sql = db.openHelper.writableDatabase
        val manifest = zip.outputStream().use { BackupArchive.write(sql, null, File(dir, "photos"), "goals-dev", sql.version, "MANUAL", now, it) }
        BackupArchive.verify(zip, manifest, sql.version)
        db.clearAllTables()
        BackupArchive.restore(sql, zip, File(dir, "photos"), sql.version) { block -> db.runInTransaction(block) }
        assertEquals(draft().why, db.goals().get(id)!!.why)
        assertEquals(id, tasks.day(today).items.single().task.goalId)
        assertTrue(tasks.day(today).items.single().done)
        assertTrue(db.goals().observeMilestones().first().isNotEmpty())
    }
}
