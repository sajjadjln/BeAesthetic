package app.lantern.backup

import app.lantern.data.db.BackupRecordEntity
import app.lantern.domain.backup.BackupNotice

/** What Today, the Settings tile and the backup screen say about backups (SPEC §8), from one rule. */
data class BackupStatus(
    val notice: BackupNotice,
    val hasFolder: Boolean,
    val lastOk: BackupRecordEntity?,
    /** Days since the last good backup (0 = today), or null with none yet. */
    val daysSince: Long?,
    /** Settings' calm line (B3): no good backup in [app.lantern.domain.backup.BackupRules.NUDGE_DAYS] days. */
    val nudge: Boolean = false,
)
