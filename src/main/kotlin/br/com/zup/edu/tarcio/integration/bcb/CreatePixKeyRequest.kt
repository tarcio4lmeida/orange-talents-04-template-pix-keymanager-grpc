package br.com.zup.edu.tarcio.integration.bcb

data class CreatePixKeyRequest(
    val keyType: String,
    val key: String,
    val bankAccount: BankAccout,
    val owner: Owner
)

data class Owner(
    val type: OwnerType,
    val name: String,
    val taxIdNumber: String
)

enum class OwnerType {
    NATURAL_PERSON, LEGAL_PERSON
}

data class BankAccout(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType: AccountType
)

enum class AccountType {
    CACC, SVGS
}