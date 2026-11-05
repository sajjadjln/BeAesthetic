package app.lantern.crash

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/** One saved crash: when, its first line (the exception) and the whole text. */
data class CrashReport(val name: String, val at: Instant, val firstLine: String, val text: String)

/** What a crash report says about the app and the phone, read once at install. */
data class CrashInfo(val versionName: String, val versionCode: Long, val android: String, val sdk: Int, val phone: String)

/**
 * The local crash log (B5, D211): one text file per crash in a private folder, the newest [KEEP] kept.
 * Nothing is sent anywhere (D63); a user copies a report from «درباره» and sends it themselves.
 *
 * Plain `java.io` only, so it runs in a JVM test. [record] runs inside the dying process, so it is
 * synchronous and never throws.
 */
class CrashLog(val dir: File) {

    /** Writes [error] to a new file, then drops the oldest beyond [KEEP]. Never throws. */
    fun record(thread: Thread, error: Throwable, info: CrashInfo?, now: Instant) {
        try {
            dir.mkdirs()
            val text = buildString {
                append("Lantern crash\n")
                append("time: ").append(now).append('\n')
                if (info != null) {
                    append("app: ").append(info.versionName).append(" (").append(info.versionCode).append(")\n")
                    append("android: ").append(info.android).append(" (API ").append(info.sdk).append(")\n")
                    append("phone: ").append(info.phone).append('\n')
                }
                append("thread: ").append(thread.name).append('\n')
                append('\n')
                append(stackTrace(error))
            }
            var file = File(dir, fileName(now, 0))
            var n = 1
            while (file.exists() && n < 100) file = File(dir, fileName(now, n++))
            file.writeText(text)
        } catch (_: Throwable) {
            // A crash log must never be the reason a crash goes wrong.
        }
        try {
            prune()
        } catch (_: Throwable) {
        }
    }

    /** Every saved crash, newest first. A file that can't be read is skipped, never fatal. */
    fun list(): List<CrashReport> = files().mapNotNull { read(it) }

    /** One crash by its file name, or null. */
    fun get(name: String): CrashReport? = files().firstOrNull { it.name == name }?.let(::read)

    /** The crash files, newest first (the backup copies them as they are). */
    fun files(): List<File> = try {
        dir.listFiles { f -> f.isFile && parseTime(f.name) != null }.orEmpty().sortedByDescending { it.name }
    } catch (_: Throwable) {
        emptyList()
    }

    fun clear() {
        files().forEach { runCatching { it.delete() } }
    }

    private fun prune() {
        files().drop(KEEP).forEach { it.delete() }
    }

    private fun read(file: File): CrashReport? = try {
        val text = file.readText()
        val at = parseTime(file.name) ?: return null
        // The exception line comes after the header's blank line; a file without one shows its first line.
        val body = text.substringAfter("\n\n", text)
        val first = body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
            ?: text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
            ?: ""
        CrashReport(file.name, at, first.take(200), text)
    } catch (_: Throwable) {
        null
    }

    companion object {
        const val KEEP = 20
        private const val PREFIX = "crash-"
        private const val SUFFIX = ".txt"

        /** `crash-<epoch ms, 15 digits>[-n].txt`: names sort by time. */
        fun fileName(at: Instant, n: Int): String =
            PREFIX + at.toEpochMilli().toString().padStart(15, '0') + (if (n > 0) "-$n" else "") + SUFFIX

        fun parseTime(name: String): Instant? {
            if (!name.startsWith(PREFIX) || !name.endsWith(SUFFIX)) return null
            val ms = name.removePrefix(PREFIX).removeSuffix(SUFFIX).substringBefore('-').toLongOrNull() ?: return null
            return Instant.ofEpochMilli(ms)
        }

        private fun stackTrace(error: Throwable): String = try {
            StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        } catch (_: Throwable) {
            error.javaClass.name
        }
    }
}

/**
 * Writes the crash, then hands it to the handler that was there before, so the system still ends the
 * process the normal way ("app has stopped").
 */
class CrashHandler(
    private val log: CrashLog,
    private val info: CrashInfo?,
    private val previous: Thread.UncaughtExceptionHandler?,
    private val now: () -> Instant = Instant::now,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, error: Throwable) {
        try {
            log.record(thread, error, info, now())
        } catch (_: Throwable) {
        }
        if (previous != null) {
            previous.uncaughtException(thread, error)
        } else {
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }
}

/** The app's crash folder: private storage, `files/crashes`. */
fun crashLog(context: android.content.Context): CrashLog = CrashLog(File(context.filesDir, "crashes"))

/** Installs [CrashHandler] once, in front of whatever handler is there. Call first thing in `App.onCreate`. */
fun installCrashHandler(context: android.content.Context) {
    try {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is CrashHandler) return
        val info = try {
            CrashInfo(
                versionName = app.lantern.BuildConfig.VERSION_NAME,
                versionCode = app.lantern.BuildConfig.VERSION_CODE.toLong(),
                android = android.os.Build.VERSION.RELEASE ?: "?",
                sdk = android.os.Build.VERSION.SDK_INT,
                phone = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            )
        } catch (_: Throwable) {
            null
        }
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(crashLog(context), info, previous))
    } catch (_: Throwable) {
    }
}
