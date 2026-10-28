package com.casasegura.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CredentialEntity::class,
        CredentialSecretEntity::class,
        CameraEntity::class,
        IpChangeEventEntity::class,
        LayoutEntity::class,
        LayoutSlotEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class CasaSeguraDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao
    abstract fun cameraDao(): CameraDao
    abstract fun layoutDao(): LayoutDao

    companion object {
        /** Versão 3 acrescenta visualizações e seus slots. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `layouts` (
                        `id` TEXT NOT NULL, `name` TEXT NOT NULL, `rows` INTEGER NOT NULL,
                        `columns` INTEGER NOT NULL, `favorite` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL, PRIMARY KEY(`id`))
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `layout_slots` (
                        `layoutId` TEXT NOT NULL, `slotIndex` INTEGER NOT NULL,
                        `cameraId` TEXT, `stream` TEXT NOT NULL,
                        PRIMARY KEY(`layoutId`, `slotIndex`))
                    """.trimIndent(),
                )
            }
        }

        /** Versão 2 acrescenta câmeras e histórico de mudança de IP. Não altera as credenciais. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cameras` (
                        `id` TEXT NOT NULL, `name` TEXT NOT NULL, `currentHost` TEXT,
                        `credentialId` TEXT, `connection` TEXT NOT NULL,
                        `identityUuid` TEXT, `identitySerial` TEXT, `identityMac` TEXT, `identityModel` TEXT,
                        `lastSeenEpochMillis` INTEGER, `location` TEXT NOT NULL, `tags` TEXT NOT NULL,
                        `group` TEXT NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`id`))
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ip_change_events` (
                        `id` TEXT NOT NULL, `cameraId` TEXT NOT NULL, `oldHost` TEXT,
                        `newHost` TEXT NOT NULL, `evidence` TEXT NOT NULL,
                        `atEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))
                    """.trimIndent(),
                )
            }
        }
    }
}
