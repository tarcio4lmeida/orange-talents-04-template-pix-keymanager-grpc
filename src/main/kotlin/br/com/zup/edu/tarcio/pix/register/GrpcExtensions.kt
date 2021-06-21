package br.com.zup.edu.tarcio.pix.register
import  br.com.zup.edu.tarcio.TipoDeChave.UNKNOWN_TIPO_CHAVE
import  br.com.zup.edu.tarcio.TipoDeConta.UNKNOWN_TIPO_DE_CONTA

import br.com.zup.edu.tarcio.RegistraChavePixRequest
import br.com.zup.edu.tarcio.pix.TipoDeChave
import br.com.zup.edu.tarcio.pix.TipoDeConta


fun RegistraChavePixRequest.toModel(): NovaChavePix {
    return NovaChavePix(
        clientId = clientId,
        tipo = when (tipoDeChave) {
            UNKNOWN_TIPO_CHAVE -> null
            else -> TipoDeChave.valueOf(tipoDeChave.name)
        },
        chave = chave,
        tipoDeConta = when (tipoDeConta) {
            UNKNOWN_TIPO_DE_CONTA -> null
            else -> TipoDeConta.valueOf(tipoDeConta.name)
        }
    )
}