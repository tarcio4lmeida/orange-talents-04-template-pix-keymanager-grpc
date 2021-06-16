package br.com.zup.edu.tarcio.pix.register

import br.com.zup.edu.tarcio.RegistraChavePixRequest
import br.com.zup.edu.tarcio.TipoDeChave
import br.com.zup.edu.tarcio.TipoDeConta
import br.com.zup.edu.tarcio.pix.TipoChave

fun RegistraChavePixRequest.toModel(): NovaChavePix {
    return NovaChavePix(
        clientId = clientId,
        tipo = when (tipoDeChave) {
            TipoDeChave.UNKNOWN_TIPO_CHAVE -> null
            else -> TipoChave.valueOf(tipoDeChave.name)
        },
        chave = chave,
        tipoDeConta = when (tipoDeConta) {
            TipoDeConta.UNKNOWN_TIPO_DE_CONTA -> null
            else -> TipoDeConta.valueOf(tipoDeConta.name)
        }
    )
}