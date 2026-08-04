package com.example.f1_kotlin.domain.auth

/**
 * Клиентские проверки email/password до вызова Firebase Auth.
 * Без сети; disposable-список — мягкий фильтр.
 */
object AuthFormValidators {
    const val MIN_PASSWORD_LENGTH = 8

    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val letterRegex = Regex("[A-Za-zА-Яа-яЁё]")
    private val digitRegex = Regex("\\d")

    /** ≥8 символов, есть буква и цифра. */
    fun isPasswordStrongEnough(password: String): Boolean {
        if (password.length < MIN_PASSWORD_LENGTH) return false
        return letterRegex.containsMatchIn(password) && digitRegex.containsMatchIn(password)
    }

    fun isEmailFormatOk(email: String): Boolean =
        emailRegex.matches(email.trim())

    fun emailDomain(email: String): String? {
        val trimmed = email.trim()
        val at = trimmed.lastIndexOf('@')
        if (at < 0 || at == trimmed.lastIndex) return null
        return trimmed.substring(at + 1).lowercase()
    }

    fun isDisposableEmail(email: String): Boolean {
        val domain = emailDomain(email) ?: return false
        if (domain in DISPOSABLE_DOMAINS) return true
        return DISPOSABLE_DOMAINS.any { blocked -> domain.endsWith(".$blocked") }
    }

    private val DISPOSABLE_DOMAINS = setOf(
        "10minutemail.com",
        "1secmail.com",
        "discard.email",
        "dispostable.com",
        "fakeinbox.com",
        "getnada.com",
        "guerrillamail.com",
        "guerrillamail.de",
        "guerrillamail.net",
        "guerrillamail.org",
        "mailinator.com",
        "mailnesia.com",
        "maildrop.cc",
        "meltmail.com",
        "moakt.com",
        "sharklasers.com",
        "spam4.me",
        "temp-mail.org",
        "tempail.com",
        "tempmail.com",
        "tempmailo.com",
        "throwawaymail.com",
        "tmpmail.net",
        "tmpmail.org",
        "trashmail.com",
        "yopmail.com",
        "yopmail.fr",
    )
}
