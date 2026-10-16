package app.beaesthetic.data.repo

import androidx.room.withTransaction
import app.beaesthetic.data.db.AppDatabase
import app.beaesthetic.data.db.CheckInEntity
import app.beaesthetic.data.db.MetricEntity
import app.beaesthetic.data.db.MetricEntryEntity
import app.beaesthetic.data.db.MilestoneEntity
import app.beaesthetic.domain.progress.Milestone
import app.beaesthetic.data.db.PhotoEntity
import app.beaesthetic.domain.progress.Metric
import app.beaesthetic.domain.progress.MetricKind
import app.beaesthetic.domain.progress.MetricUnit
import app.beaesthetic.domain.progress.Reading
import app.beaesthetic.domain.progress.SuggestedMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

enum class Pose { FRONT, SIDE, BACK }

/** Everything Progress shows: metrics with their readings, finished check-ins and photos. */
data class ProgressData(
    val metrics: List<Metric>,
    val readings: Map<Long, List<Reading>>,
    /** Finished check-ins by index. */
    val checkIns: Map<Int, CheckInEntity>,
    /** Photos by check-in, then pose. */
    val photos: Map<Int, Map<Pose, PhotoEntity>>,
    val milestones: List<Milestone> = emptyList(),
)

/** Metrics, readings, check-ins and photos (slice 7a). */
class ProgressRepository(private val db: AppDatabase, private val photos: PhotoStore) {

    private val dao = db.progress()

    fun observe(): Flow<ProgressData> = combine(
        dao.observeMetrics(),
        dao.observeEntries(),
        dao.observeCheckIns(),
        dao.observePhotos(),
        dao.observeMilestones(),
    ) { metrics, entries, checkIns, photoRows, milestones ->
        ProgressData(
            metrics = metrics.map { it.toDomain() },
            readings = entries.groupBy({ it.metricId }, { Reading(it.date, it.value, it.guess) }),
            checkIns = checkIns.filter { it.doneAt != null }.associateBy { it.idx },
            photos = photoRows.groupBy { it.checkInIdx }.mapValues { (_, rows) ->
                rows.mapNotNull { row -> runCatching { Pose.valueOf(row.pose) }.getOrNull()?.let { it to row } }.toMap()
            },
            milestones = milestones.map { Milestone(it.id, it.title, it.metricId, it.target, it.reachedOn) },
        )
    }

    fun observeMetrics(): Flow<List<Metric>> = dao.observeMetrics().map { rows -> rows.map { it.toDomain() } }

    /** A suggested metric, one tap (D100). [name] comes from string resources. */
    suspend fun addSuggested(suggested: SuggestedMetric, name: String) {
        if (dao.metrics().any { it.key == suggested.key }) return
        dao.insertMetric(
            MetricEntity(
                kind = suggested.kind.name, key = suggested.key, name = name, unit = suggested.unit.name,
                higherIsBetter = suggested.higherIsBetter, sortOrder = dao.maxSortOrder() + 1,
            ),
        )
    }

    /** The user's own metric, or an edit of one ([id] set). The key of a suggested one is kept. */
    suspend fun save(id: Long?, kind: MetricKind, name: String, unit: MetricUnit, higherIsBetter: Boolean) {
        val existing = id?.let { dao.metric(it) }
        if (existing == null) {
            dao.insertMetric(MetricEntity(kind = kind.name, name = name, unit = unit.name, higherIsBetter = higherIsBetter, sortOrder = dao.maxSortOrder() + 1))
        } else {
            dao.updateMetric(existing.copy(kind = kind.name, name = name, unit = unit.name, higherIsBetter = higherIsBetter))
        }
    }

    suspend fun remove(id: Long, at: Instant) = dao.archiveMetric(id, at)

    /** One reading a day per metric: the same day again replaces it. */
    suspend fun record(metricId: Long, date: LocalDate, value: Double, guess: Double? = null) {
        val existing = dao.entry(metricId, date)
        dao.upsertEntry(MetricEntryEntity(id = existing?.id ?: 0, metricId = metricId, date = date, value = value, guess = guess))
    }

    suspend fun deleteReading(metricId: Long, date: LocalDate) = dao.deleteEntry(metricId, date)

    /** Copies, downscales and strips the photo (ARCHITECTURE §10), then points the check-in's pose at it. */
    suspend fun savePhoto(idx: Int, pose: Pose, source: android.net.Uri, now: Instant) {
        val name = photos.import(source, "$idx-${pose.name.lowercase()}.jpg")
        dao.upsertPhoto(PhotoEntity(idx, pose.name, name, now))
    }

    suspend fun deletePhoto(idx: Int, pose: Pose) {
        dao.deletePhoto(idx, pose.name)
        photos.delete("$idx-${pose.name.lowercase()}.jpg")
    }

    /** A new milestone ([id] null) or an edit; the tick of a hand milestone is kept unless [reachedOn] says. */
    suspend fun saveMilestone(id: Long?, title: String, metricId: Long?, target: Double?, reachedOn: LocalDate?) {
        val order = if (id == null) dao.maxMilestoneOrder() + 1 else 0
        dao.upsertMilestone(MilestoneEntity(id ?: 0, title, metricId, target, reachedOn, order))
    }

    suspend fun deleteMilestone(id: Long) = dao.deleteMilestone(id)

    suspend fun finishCheckIn(idx: Int, date: LocalDate, now: Instant) = db.withTransaction {
        dao.upsertCheckIn(CheckInEntity(idx, date, now))
    }

    private fun MetricEntity.toDomain() = Metric(
        id = id,
        kind = runCatching { MetricKind.valueOf(kind) }.getOrDefault(MetricKind.STRENGTH),
        key = key,
        name = name,
        unit = runCatching { MetricUnit.valueOf(unit) }.getOrDefault(MetricUnit.REPS),
        higherIsBetter = higherIsBetter,
    )
}
