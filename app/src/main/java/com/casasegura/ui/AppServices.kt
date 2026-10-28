package com.casasegura.ui

import com.casasegura.core.cameras.CameraRepository
import com.casasegura.core.cameras.IpChangeMonitor
import com.casasegura.core.credentials.CredentialService
import com.casasegura.core.layouts.LayoutService
import com.casasegura.protocol.onvif.WsDiscoveryClient

/** Dependências entregues às telas. Criadas uma vez pela Application. */
data class AppServices(
    val credentials: CredentialService,
    val cameras: CameraRepository,
    val layouts: LayoutService,
    val discovery: WsDiscoveryClient,
    val ipMonitor: IpChangeMonitor,
)
