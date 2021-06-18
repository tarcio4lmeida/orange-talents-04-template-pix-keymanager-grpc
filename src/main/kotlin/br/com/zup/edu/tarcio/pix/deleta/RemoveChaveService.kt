package br.com.zup.edu.tarcio.pix.deleta

import br.com.zup.edu.tarcio.integration.bcb.BcbClient
import br.com.zup.edu.tarcio.integration.bcb.DeletePixKeyRequest
import br.com.zup.edu.tarcio.pix.ChavePixInexistenteException
import br.com.zup.edu.tarcio.pix.ChavePixRepository
import br.com.zup.edu.tarcio.pix.PermissaoNegadaException
import br.com.zup.edu.tarcio.shared.validation.ValidUUID
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

@Validated
@Singleton
class RemoveChaveService(
    @Inject val repository: ChavePixRepository,
    @Inject val bcbClient: BcbClient
) {

    @Transactional
    fun remove(
        @NotBlank @ValidUUID(message = "Cliente ID com formato invalido") clientId: String?,
        @NotBlank @ValidUUID(message = "Pix ID com formato invalido") pixId: String?
    ) {
        val uuidPixId = UUID.fromString(pixId)
        val uuidClientId = UUID.fromString(clientId)

        val chave = repository.findByPixId(uuidPixId)

        if (chave.isEmpty)
            throw ChavePixInexistenteException("Chave Pix '${uuidPixId}' não existe")

        if (chave.get().clientId.toString() != uuidClientId.toString()) {
            throw PermissaoNegadaException("Cliente não tem permissão para apagar essa chave")
        }

        val request = DeletePixKeyRequest(
            key = chave.get().tipoDeConta.toString(),
            participant = chave.get().conta.ispb
        )

        val bcbResponse = bcbClient.deletaChavePixBcb(request, chave.get().chave)
        check(bcbResponse.status != HttpStatus.NOT_FOUND) { "Chave não existe" }
        check(bcbResponse.status != HttpStatus.FORBIDDEN) { "Não foi possivel cadastrar chave no BCB" }
        check(bcbResponse.status == HttpStatus.OK) { "Falha na remoção de chave no BCB" }

        repository.deleteById(chave.get().id)
    }
}