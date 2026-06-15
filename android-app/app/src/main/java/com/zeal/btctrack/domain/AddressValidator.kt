package com.zeal.btctrack.domain

object AddressValidator {
    private val pattern = Regex("^(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,87}$")

    fun isValid(address: String): Boolean = address.trim().matches(pattern)
}
