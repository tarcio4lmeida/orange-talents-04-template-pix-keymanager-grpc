package br.com.zup.edu.tarcio.pix.register

import br.com.zup.edu.tarcio.TipoDeConta
import br.com.zup.edu.tarcio.integration.bcb.*
import br.com.zup.edu.tarcio.pix.ChavePix
import br.com.zup.edu.tarcio.pix.ContaAssociada
import br.com.zup.edu.tarcio.pix.TipoChave
import br.com.zup.edu.tarcio.shared.validation.ValidPixKey
import br.com.zup.edu.tarcio.shared.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@ValidPixKey
@Introspected
data class NovaChavePix(
    @ValidUUID
    @field:NotBlank
    val clientId: String?,

    @field:NotNull
    val tipo: TipoChave?,

    @field:Size(max = 77)
    val chave: String?,

    @field:NotNull
    val tipoDeConta: TipoDeConta?
) {

    fun toModel(conta: ContaAssociada, responseBcb: CreatePixKeyResponse): ChavePix {
        return ChavePix(
            clientId = UUID.fromString(this.clientId),
            tipo = TipoChave.valueOf(this.tipo!!.name),
            chave = responseBcb.key,
            tipoDeConta = TipoDeConta.valueOf(this.tipoDeConta!!.name),
            conta = conta
        )
    }

    fun toBcbModel(conta: ContaAssociada): CreatePixKeyRequest {
        return CreatePixKeyRequest(
            keyType = this.tipo.toString(),
            key = this.chave!!,  // Verificar como mandar nulo em caso de chave Random
            bankAccount = BankAccout(
                participant = conta.ispb,
                branch = conta.agencia,
                accountNumber = conta.numeroDaConta,
                accountType = if (this.tipoDeConta == TipoDeConta.CONTA_CORRENTE) AccountType.CACC else AccountType.SVGS
            ),
            owner = Owner(
                type = OwnerType.NATURAL_PERSON,
                name = conta.nomeDoTitular,
                taxIdNumber = conta.cpfDoTitular
            )
        )
    }
}