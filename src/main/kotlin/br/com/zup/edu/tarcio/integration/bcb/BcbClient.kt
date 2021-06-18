package br.com.zup.edu.tarcio.integration.bcb

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

@Client(value = "\${bcb.url}")
interface BcbClient {

    @Post(
        consumes = [MediaType.APPLICATION_XML],
        produces = [MediaType.APPLICATION_XML]
    )
    fun cadastraChaveBcb(@Body request: CreatePixKeyRequest): HttpResponse<CreatePixKeyResponse>

    @Delete(
        consumes = [MediaType.APPLICATION_XML],
        produces = [MediaType.APPLICATION_XML],
        value = "/{key}"
    )
    fun deletaChavePixBcb(
        @Body request: DeletePixKeyRequest,
        @PathVariable key: String
    ): HttpResponse<DeletePixKeyResponse>
}