package app.lantern.crash

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.Instant

/** The local crash log (B5, D211): plain JVM, no Android. */
class CrashLogTest {

    private lateinit var dir: File
    private lateinit var log: CrashLog
    private val info = CrashInfo("0.9.6", 906, "14", 34, "Xiaomi 22041219PG")
    private val t0 = Instant.parse("2026-11-05T10:00:00Z")

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("crashes").toFile()
        log = CrashLog(File(dir, "crashes"))
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `a crash is written with the phone, the version and the full stack trace`() {
        val cause = IllegalArgumentException("inner")
        log.record(Thread.currentThread(), IllegalStateException("boom", cause), info, t0)

        val all = log.list()
        assertEquals(1, all.size)
        val crash = all.single()
        assertEquals(t0, crash.at)
        assertEquals("java.lang.IllegalStateException: boom", crash.firstLine)
        assertTrue(crash.text.contains("app: 0.9.6 (906)"))
        assertTrue(crash.text.contains("android: 14 (API 34)"))
        assertTrue(crash.text.contains("phone: Xiaomi 22041219PG"))
        assertTrue(crash.text.contains("thread: ${Thread.currentThread().name}"))
        assertTrue(crash.text.contains("Caused by: java.lang.IllegalArgumentException: inner"))
        assertTrue(crash.text.contains("at app.lantern.crash.CrashLogTest"))
        assertEquals(crash, log.get(crash.name))
    }

    @Test
    fun `only the newest 20 are kept, newest first`() {
        repeat(25) { i -> log.record(Thread.currentThread(), RuntimeException("crash $i"), info, t0.plusSeconds(i * 60L)) }

        val all = log.list()
        assertEquals(CrashLog.KEEP, all.size)
        assertEquals("java.lang.RuntimeException: crash 24", all.first().firstLine)
        assertEquals("java.lang.RuntimeException: crash 5", all.last().firstLine)
        assertEquals(all.sortedByDescending { it.at }, all)
        assertEquals(CrashLog.KEEP, log.dir.listFiles()!!.size)
    }

    @Test
    fun `two crashes in the same millisecond both stay`() {
        log.record(Thread.currentThread(), RuntimeException("one"), info, t0)
        log.record(Thread.currentThread(), RuntimeException("two"), info, t0)
        assertEquals(2, log.list().size)
    }

    @Test
    fun `a corrupt or foreign file never breaks the list`() {
        log.record(Thread.currentThread(), RuntimeException("real"), null, t0)
        // Garbage bytes under a valid name, a file with no header, a stranger, and a folder with a crash's name.
        File(log.dir, CrashLog.fileName(t0.plusSeconds(60), 0)).writeBytes(byteArrayOf(-1, -2, 0, 7, -128))
        File(log.dir, CrashLog.fileName(t0.plusSeconds(120), 0)).writeText("just one line")
        File(log.dir, "notes.txt").writeText("not a crash")
        File(log.dir, CrashLog.fileName(t0.plusSeconds(180), 0)).mkdirs()

        val all = log.list()
        assertEquals(3, all.size)
        assertEquals("just one line", all[0].firstLine)
        assertEquals("java.lang.RuntimeException: real", all.last().firstLine)

        // Writing still works next to them.
        log.record(Thread.currentThread(), RuntimeException("after"), null, t0.plusSeconds(240))
        assertEquals("java.lang.RuntimeException: after", log.list().first().firstLine)
    }

    @Test
    fun `record never throws, even when the folder can't be made`() {
        val blocked = File(dir, "blocked").apply { writeText("a file where the folder should be") }
        CrashLog(File(blocked, "crashes")).record(Thread.currentThread(), RuntimeException("x"), info, t0)
        assertTrue(CrashLog(File(blocked, "crashes")).list().isEmpty())
    }

    @Test
    fun `clear removes every crash`() {
        repeat(3) { i -> log.record(Thread.currentThread(), RuntimeException("$i"), info, t0.plusSeconds(i.toLong())) }
        log.clear()
        assertTrue(log.list().isEmpty())
        assertNull(log.get(CrashLog.fileName(t0, 0)))
    }

    @Test
    fun `the handler writes the crash, then passes it on`() {
        var passed: Throwable? = null
        val previous = Thread.UncaughtExceptionHandler { _, e -> passed = e }
        val error = RuntimeException("handled")
        CrashHandler(log, info, previous) { t0 }.uncaughtException(Thread.currentThread(), error)

        assertEquals(error, passed)
        assertEquals("java.lang.RuntimeException: handled", log.list().single().firstLine)
    }
}
