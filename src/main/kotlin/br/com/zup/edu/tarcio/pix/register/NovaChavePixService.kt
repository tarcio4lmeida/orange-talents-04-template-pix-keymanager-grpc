package br.com.zup.edu.tarcio.pix.register


import br.com.zup.edu.tarcio.pix.ChavePix
import br.com.zup.edu.tarcio.pix.ChavePixExistenteException
import br.com.zup.edu.tarcio.pix.ChavePixRepository
import br.com.zup.edu.tarcio.integration.itau.ItauClient
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class NovaChavePixService(
    @Inject val repository: ChavePixRepository,
    @Inject val itauClient: ItauClient
) {

    private val logger = LoggerFactory.getLogger((this::class.java))

    @Transactional
    fun registra(@Valid novaChave: NovaChavePix): ChavePix {

        // 1. Verifica se chave já existe no sistema
        if (repository.existsByChave(novaChave.chave))
            throw ChavePixExistenteException("Chave Pix '${novaChave.chave}' existente")

        // 2. Busca dados da conta no ERP do Itau
        val response = itauClient.buscaContaPorTipo(
            novaChave.clientId!!,
            novaChave.tipoDeConta!!.name
        )

        val conta = response.body()?.toModel()?: throw IllegalStateException("Cliente não encontrado no Itaú")

        // 3. Grava no banco de dados
        val chave = novaChave.toModel(conta)
        repository.save(chave)

        return chave
    }

}