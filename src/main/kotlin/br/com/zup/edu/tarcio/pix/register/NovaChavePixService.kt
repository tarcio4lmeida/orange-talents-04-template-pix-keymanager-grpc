package br.com.zup.edu.tarcio.pix.register


import br.com.zup.edu.tarcio.integration.bcb.BcbClient
import br.com.zup.edu.tarcio.pix.ChavePix
import br.com.zup.edu.tarcio.pix.ChavePixRepository
import br.com.zup.edu.tarcio.integration.itau.ItauClient
import br.com.zup.edu.tarcio.pix.ChavePixExistenteException
import io.micronaut.validation.Validated
import io.micronaut.http.HttpStatus
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class NovaChavePixService(
    @Inject val repository: ChavePixRepository,
    @Inject val itauClient: ItauClient,
    @Inject val bcbClient: BcbClient
) {

    @Transactional
    fun registra(@Valid novaChave: NovaChavePix): ChavePix {

        // 1. Verifica se chave já existe no sistema
        if (repository.existsByChave(novaChave.chave))
            throw ChavePixExistenteException("Chave Pix '${novaChave.chave}' existente")

        // 2. Busca dados da conta no ERP do Itau
        val responseItau  = itauClient.buscaContaPorTipo(
            novaChave.clientId!!,
            novaChave.tipoDeConta!!.name
        )

        val conta = responseItau.body()!!.toModel()

        // 3 Registra a Chave Globalmente no BCB
        val responseBcb = bcbClient.cadastraChaveBcb(novaChave.toBcbModel(conta))
        check(responseBcb.status != HttpStatus.UNPROCESSABLE_ENTITY) { "Chave Pix já cadastrada no BCB" }
        check(responseBcb.status == HttpStatus.CREATED) { "Não foi possivel cadastrar chave no BCB" }

        // 4 Grava no banco de dados
        val chave = novaChave.toModel(conta, responseBcb.body()!!)
        repository.save(chave)

        return chave
    }

}