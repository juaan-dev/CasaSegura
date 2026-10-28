package com.casasegura.core.cameras

import com.casasegura.core.identity.IdentityResolver
import com.casasegura.core.identity.IpChangeDecision
import com.casasegura.core.identity.IpChangeEvaluator
import com.casasegura.core.identity.MatchResult
import com.casasegura.core.identity.Observation
import java.util.UUID

/**
 * Aplica a verificação de mudança de IP após uma varredura (seção 2.6 do plano).
 * Atualiza o endereço só quando a decisão é segura; o resto vira pendência para o usuário.
 */
class IpChangeMonitor(
    private val repository: CameraRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {

    data class Report(
        val applied: List<IpChangeEvent>,
        val pending: List<IpChangeDecision.NeedsConfirmation>,
    )

    suspend fun scan(observations: List<Observation>): Report {
        val cameras = repository.list()
        val applied = mutableListOf<IpChangeEvent>()
        val pending = mutableListOf<IpChangeDecision.NeedsConfirmation>()
        val now = clock()

        for (camera in cameras) {
            val decisions = observations.map { IpChangeEvaluator.evaluate(camera, cameras, it) }

            // Confirma que a câmera ainda está no endereço atual, mesmo sem mudança.
            val stillHere = observations.any { obs ->
                obs.host == camera.currentHost &&
                    IdentityResolver.resolve(cameras, obs).let { it is MatchResult.Matched && it.cameraId == camera.id }
            }
            if (stillHere) repository.save(camera.copy(lastSeenEpochMillis = now))

            val autoUpdates = decisions.filterIsInstance<IpChangeDecision.AutoUpdate>()
            val needsConfirmation = decisions.filterIsInstance<IpChangeDecision.NeedsConfirmation>()
            pending += needsConfirmation

            when {
                // Duas observações apontando para a mesma câmera não podem ser aplicadas sem o usuário decidir.
                autoUpdates.size > 1 -> pending += IpChangeDecision.NeedsConfirmation(
                    cameraId = camera.id,
                    candidateHosts = autoUpdates.map { it.newHost },
                    reason = "Mais de um endereço responde como esta câmera",
                )
                autoUpdates.size == 1 -> {
                    val update = autoUpdates.single()
                    val updated = camera.copy(currentHost = update.newHost, lastSeenEpochMillis = now)
                    repository.save(updated)
                    val event = IpChangeEvent(
                        id = idGenerator(),
                        cameraId = camera.id,
                        oldHost = update.oldHost,
                        newHost = update.newHost,
                        evidence = update.evidence,
                        atEpochMillis = now,
                    )
                    repository.recordIpChange(event)
                    applied += event
                }
            }
        }
        return Report(applied = applied, pending = pending)
    }
}
