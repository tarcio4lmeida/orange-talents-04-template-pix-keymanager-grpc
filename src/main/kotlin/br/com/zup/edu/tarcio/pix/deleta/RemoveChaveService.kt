package br.com.zup.edu.tarcio.pix.deleta

import br.com.zup.edu.tarcio.pix.ChavePixInexistenteException
import br.com.zup.edu.tarcio.pix.ChavePixRepository
import br.com.zup.edu.tarcio.pix.PermissaoNegadaException
import br.com.zup.edu.tarcio.shared.validation.ValidUUID
import io.micronaut.validation.Validated
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

@Validated
@Singleton
class RemoveChaveService(
    @Inject val repository: ChavePixRepository
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

        repository.deleteById(chave.get().id)
    }
}