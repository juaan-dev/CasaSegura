package com.casasegura.core.identity

import com.casasegura.core.model.Camera

/** O que um dispositivo anunciou ao ser encontrado em um endereço. */
data class Observation(
    val host: String,
    val uuid: String? = null,
    val serial: String? = null,
    val mac: String? = null,
    val model: String? = null,
    /** True quando a identidade foi confirmada pelo protocolo (ex.: ONVIF autenticado), não só anunciada. */
    val verifiedByProtocol: Boolean = false,
)

/** Evidência usada para associar um dispositivo a uma câmera conhecida. Em ordem de confiança. */
enum class IdentityEvidence {
    UUID,
    SERIAL,
    MAC,
}

sealed interface MatchResult {
    data class Matched(val cameraId: String, val evidence: IdentityEvidence) : MatchResult
    /** Mais de uma câmera compatível: nunca escolher automaticamente. */
    data class Ambiguous(val candidateIds: List<String>, val reason: String) : MatchResult
    data object NoMatch : MatchResult
}

/**
 * Correlaciona observações com câmeras conhecidas (seção 2.6 do plano).
 * Usa o identificador mais forte disponível. Modelo sozinho gera apenas candidatos, nunca uma associação.
 */
object IdentityResolver {

    fun resolve(known: List<Camera>, observation: Observation): MatchResult {
        strongMatch(known, observation, IdentityEvidence.UUID) { it.identity.uuid }?.let { return it }
        strongMatch(known, observation, IdentityEvidence.SERIAL) { it.identity.serial }?.let { return it }
        strongMatch(known, observation, IdentityEvidence.MAC) { it.identity.mac?.uppercase() }?.let { return it }

        val modelCandidates = known.filter {
            observation.model != null && it.identity.model.equals(observation.model, ignoreCase = true)
        }
        return when {
            modelCandidates.isEmpty() -> MatchResult.NoMatch
            else -> MatchResult.Ambiguous(
                candidateIds = modelCandidates.map { it.id },
                reason = "Apenas o modelo coincide; confirme qual câmera é",
            )
        }
    }

    private inline fun strongMatch(
        known: List<Camera>,
        observation: Observation,
        evidence: IdentityEvidence,
        valueOf: (Camera) -> String?,
    ): MatchResult? {
        val value = when (evidence) {
            IdentityEvidence.UUID -> observation.uuid
            IdentityEvidence.SERIAL -> observation.serial
            IdentityEvidence.MAC -> observation.mac
        } ?: return null

        val candidates = known.filter { valueOf(it)?.equals(value, ignoreCase = true) == true }
        return when (candidates.size) {
            0 -> null
            1 -> MatchResult.Matched(candidates.single().id, evidence)
            else -> MatchResult.Ambiguous(
                candidateIds = candidates.map { it.id },
                reason = "Mais de uma câmera tem o mesmo ${evidence.name.lowercase()}",
            )
        }
    }
}
