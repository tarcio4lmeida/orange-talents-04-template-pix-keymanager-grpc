package br.com.zup.edu.tarcio.integration.bcb

import java.time.LocalDateTime

data class CreatePixKeyResponse(
    val keyType: String,
    val key: String,
    val bankAccount: BankAccout,
    val owner: Owner,
    val createdAt: LocalDateTime
)
