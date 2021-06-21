package br.com.zup.edu.tarcio.pix.register

import br.com.zup.edu.tarcio.pix.ChavePix
import br.com.zup.edu.tarcio.pix.ContaAssociada
import br.com.zup.edu.tarcio.pix.TipoDeChave
import br.com.zup.edu.tarcio.pix.TipoDeConta
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
    val tipo: TipoDeChave?,

    @field:Size(max = 77)
    val chave: String?,

    @field:NotNull
    val tipoDeConta: TipoDeConta?
) {

    fun toModel(conta: ContaAssociada): ChavePix {
        return ChavePix(
            clientId = UUID.fromString(this.clientId),
            tipo = TipoDeChave.valueOf(this.tipo!!.name),
            chave = if (this.tipo == TipoDeChave.RANDOM) UUID.randomUUID().toString() else this.chave!!,
            tipoDeConta = TipoDeConta.valueOf(this.tipoDeConta!!.name),
            conta = conta
        )
    }
}