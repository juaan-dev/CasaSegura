package com.casasegura.core.security

/**
 * Cifra usada para segredos em repouso. A implementação real usa Android Keystore;
 * os testes unitários usam uma implementação falsa.
 */
interface SecretCipher {
    /** Retorna o material cifrado (inclui o IV). */
    fun encrypt(plain: ByteArray): ByteArray

    /** Recupera o texto original. Lança exceção se o material foi adulterado. */
    fun decrypt(blob: ByteArray): ByteArray
}
