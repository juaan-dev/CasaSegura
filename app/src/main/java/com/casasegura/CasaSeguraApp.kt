package com.casasegura

import android.app.Application
import androidx.room.Room
import com.casasegura.core.cameras.CameraRepository
import com.casasegura.core.credentials.CredentialService
import com.casasegura.core.database.CasaSeguraDatabase
import com.casasegura.core.database.RoomCameraRepository
import com.casasegura.core.database.RoomCredentialStore
import com.casasegura.core.security.KeystoreSecretCipher

/**
 * Composição manual das dependências. Simples de propósito: o projeto ainda é pequeno
 * e não justifica um framework de injeção (ver docs/architecture/decisions).
 */
class CasaSeguraApp : Application() {

    private val database: CasaSeguraDatabase by lazy {
        Room.databaseBuilder(this, CasaSeguraDatabase::class.java, "casasegura.db")
            .addMigrations(CasaSeguraDatabase.MIGRATION_1_2)
            .build()
    }

    val credentialService: CredentialService by lazy {
        CredentialService(
            store = RoomCredentialStore(
                dao = database.credentialDao(),
                cipher = KeystoreSecretCipher(),
            ),
        )
    }

    val cameraRepository: CameraRepository by lazy {
        RoomCameraRepository(database.cameraDao())
    }
}
