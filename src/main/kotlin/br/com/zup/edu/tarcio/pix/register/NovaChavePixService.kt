package br.com.zup.edu.tarcio.pix.register


import br.com.zup.edu.tarcio.integration.bcb.BcbClient
import br.com.zup.edu.tarcio.integration.bcb.CreatePixKeyRequest
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

    private val logger = LoggerFactory.getLogger((this::class.java))

    @Transactional
    fun registra(@Valid novaChave: NovaChavePix): ChavePix {

        // 1. Verifica se chave já existe na API
        if (repository.existsByChave(novaChave.chave))
            throw ChavePixExistenteException("Chave Pix '${novaChave.chave}' existente")

        // 2. Busca dados da conta no ERP do Itau
        val responseItau = itauClient.buscaContaPorTipo(
            novaChave.clientId!!,
            novaChave.tipoDeConta!!.name
        )
        check(responseItau.status != HttpStatus.NOT_FOUND) { "Cliente não encontrado no Itaú" }
        check(responseItau.status == HttpStatus.OK) { "Erro ao buscar dados da conta no Itaú" }

        val conta = responseItau.body()!!.toModel()

        // 3. grava no banco de dados
        val chave = novaChave.toModel(conta)
        repository.save(chave)

        // 4. registra chave no BCB
        val bcbResponse = bcbClient.cadastraChaveBcb(CreatePixKeyRequest.of(chave))
        check(bcbResponse.status != HttpStatus.UNPROCESSABLE_ENTITY) { "Chave Pix já cadastrada no BCB" }
        check(bcbResponse.status == HttpStatus.CREATED) { "Não foi possivel cadastrar chave no BCB" }

        chave.atualiza(bcbResponse.body()!!.key)

        return chave
    }

}