package com.casasegura.core.security

/**
 * Redação de segredos para exibição e logs (seção 2.3 e 7 do plano).
 * Não revela o tamanho da senha: a máscara tem sempre o mesmo comprimento.
 */
object SecretMasker {

    const val MASK = "••••••••"

    fun mask(@Suppress("UNUSED_PARAMETER") secret: String?): String = MASK

    /**
     * Remove usuário e senha de uma URL antes de registrar em log ou diagnóstico.
     * Exemplo: usuário e senha da URL RTSP são trocados por uma máscara, mantendo o host e a porta.
     */
    fun redactUrl(url: String): String {
        val withoutUserInfo = url.replace(Regex("://[^/@\\s]+@"), "://***@")
        return withoutUserInfo.replace(Regex("(?i)([?&](?:password|pwd|pass|token)=)[^&\\s]*"), "$1***")
    }
}
