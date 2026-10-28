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
    ],
    version = 2,
    exportSchema = true,
)
abstract class CasaSeguraDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao
    abstract fun cameraDao(): CameraDao

    companion object {
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
