package com.casasegura.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CredentialEntity::class, CredentialSecretEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CasaSeguraDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao
}
