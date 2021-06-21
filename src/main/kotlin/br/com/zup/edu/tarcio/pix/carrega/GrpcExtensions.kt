package br.com.zup.edu.tarcio.pix.carrega

import br.com.zup.edu.tarcio.CarregaChavePixRequest
import br.com.zup.edu.tarcio.CarregaChavePixRequest.FiltroCase.*

import javax.validation.ConstraintViolationException
import javax.validation.Validator


fun CarregaChavePixRequest.toModel(validator: Validator): Filtro { // 1

    val filtro = when(filtroCase!!) { // 1
        PIXID -> pixId.let { // 1
            Filtro.PorPixId(clientId = it.clientId, pixId = it.pixId) // 1
        }
        CHAVE -> Filtro.PorChave(chave) // 2
        FILTRO_NOT_SET -> Filtro.Invalido() // 2
    }

    val violations = validator.validate(filtro)
    if (violations.isNotEmpty()) {
        throw ConstraintViolationException(violations);
    }

    return filtro
}