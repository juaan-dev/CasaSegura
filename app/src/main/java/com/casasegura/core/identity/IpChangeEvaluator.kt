package com.casasegura.core.identity

import com.casasegura.core.model.Camera

/**
 * Decisão sobre uma câmera conhecida cujo IP pode ter mudado (seção 2.6 do plano).
 * Atualiza automaticamente só com identificador forte e confirmado pelo protocolo.
 * Em qualquer dúvida, pede confirmação.
 */
sealed interface IpChangeDecision {
    data object Unchanged : IpChangeDecision

    data class AutoUpdate(
        val cameraId: String,
        val oldHost: String?,
        val newHost: String,
        val evidence: IdentityEvidence,
    ) : IpChangeDecision

    data class NeedsConfirmation(
        val cameraId: String,
        val candidateHosts: List<String>,
        val reason: String,
    ) : IpChangeDecision

    /** O dispositivo observado pertence a outra câmera conhecida. */
    data class BelongsToOtherCamera(val otherCameraId: String, val host: String) : IpChangeDecision
}

object IpChangeEvaluator {

    /**
     * @param camera câmera que está sendo verificada.
     * @param allCameras todas as câmeras conhecidas, para detectar conflitos entre elas.
     * @param observation dispositivo encontrado na rede.
     * @param oldHostObservation o que responde no IP antigo agora, se for possível consultar.
     */
    fun evaluate(
        camera: Camera,
        allCameras: List<Camera>,
        observation: Observation,
        oldHostObservation: Observation? = null,
    ): IpChangeDecision {
        if (observation.host == camera.currentHost) return IpChangeDecision.Unchanged

        // Se o IP antigo ainda responde com a mesma identidade, há dois endereços para uma câmera só.
        if (oldHostObservation != null && camera.currentHost != null) {
            val oldMatch = IdentityResolver.resolve(allCameras, oldHostObservation)
            if (oldMatch is MatchResult.Matched && oldMatch.cameraId == camera.id) {
                return IpChangeDecision.NeedsConfirmation(
                    cameraId = camera.id,
                    candidateHosts = listOf(camera.currentHost, observation.host),
                    reason = "O IP antigo ainda responde como esta câmera",
                )
            }
        }

        return when (val match = IdentityResolver.resolve(allCameras, observation)) {
            is MatchResult.Matched -> when {
                match.cameraId != camera.id -> IpChangeDecision.BelongsToOtherCamera(match.cameraId, observation.host)
                !observation.verifiedByProtocol -> IpChangeDecision.NeedsConfirmation(
                    cameraId = camera.id,
                    candidateHosts = listOf(observation.host),
                    reason = "Identidade anunciada mas não confirmada pelo protocolo",
                )
                else -> IpChangeDecision.AutoUpdate(
                    cameraId = camera.id,
                    oldHost = camera.currentHost,
                    newHost = observation.host,
                    evidence = match.evidence,
                )
            }
            is MatchResult.Ambiguous -> IpChangeDecision.NeedsConfirmation(
                cameraId = camera.id,
                candidateHosts = listOf(observation.host),
                reason = match.reason,
            )
            MatchResult.NoMatch -> IpChangeDecision.Unchanged
        }
    }
}
