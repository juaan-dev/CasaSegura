package com.casasegura

import android.app.Application
import androidx.room.Room
import com.casasegura.core.cameras.CameraRepository
import com.casasegura.core.cameras.IpChangeMonitor
import com.casasegura.core.credentials.CredentialService
import com.casasegura.core.database.CasaSeguraDatabase
import com.casasegura.core.database.RoomCameraRepository
import com.casasegura.core.database.RoomCredentialStore
import com.casasegura.core.database.RoomLayoutRepository
import com.casasegura.core.layouts.LayoutService
import com.casasegura.core.security.KeystoreSecretCipher
import com.casasegura.protocol.onvif.WsDiscoveryClient
import com.casasegura.ui.AppServices

/**
 * Composição manual das dependências. Simples de propósito: o projeto ainda é pequeno
 * e não justifica um framework de injeção (ver docs/architecture/decisions).
 */
class CasaSeguraApp : Application() {

    private val database: CasaSeguraDatabase by lazy {
        Room.databaseBuilder(this, CasaSeguraDatabase::class.java, "casasegura.db")
            .addMigrations(CasaSeguraDatabase.MIGRATION_1_2, CasaSeguraDatabase.MIGRATION_2_3)
            .build()
    }

    private val cameraRepository: CameraRepository by lazy { RoomCameraRepository(database.cameraDao()) }

    val services: AppServices by lazy {
        AppServices(
            layouts = LayoutService(RoomLayoutRepository(database.layoutDao())),
            credentials = CredentialService(
                store = RoomCredentialStore(
                    dao = database.credentialDao(),
                    cipher = KeystoreSecretCipher(),
                ),
            ),
            cameras = cameraRepository,
            discovery = WsDiscoveryClient(this),
            ipMonitor = IpChangeMonitor(cameraRepository),
        )
    }
}
