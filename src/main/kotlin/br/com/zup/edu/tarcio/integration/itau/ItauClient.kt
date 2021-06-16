package br.com.zup.edu.tarcio.integration.itau

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client

@Client(value = "\${itau.url}")
interface ItauClient {

    @Get("/{clientId}/contas")
    fun buscaContaPorTipo(
        @PathVariable clientId: String,
        @QueryValue tipo: String
    ): HttpResponse<DadosDaContaResponse>

}