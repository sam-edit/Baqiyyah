package com.example.data.model

data class BankConfig(
    val id: String = "",
    val displayName: String = "",
    val accountHolder: String = "",
    val accountNumber: String = "",
    val iban: String = "",
    val isEnabled: Boolean = true,
    val qrImageUrl: String = ""
)
