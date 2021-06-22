package br.com.zup.edu.tarcio.pix.lista

import br.com.zup.edu.tarcio.KeyManagerListaServiceGrpc

import br.com.zup.edu.tarcio.ListaChavesPixRequest
import br.com.zup.edu.tarcio.pix.*


import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasSize

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

@MicronautTest(transactional = false)
internal class ListaChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerListaServiceGrpc.KeyManagerListaServiceBlockingStub,
) {


    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }


    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setup() {
        repository.save(chave(tipo = TipoDeChave.EMAIL, chave = "rafael.ponte@zup.com.br", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = TipoDeChave.RANDOM, chave = "randomkey-2", clienteId = UUID.randomUUID()))
        repository.save(chave(tipo = TipoDeChave.RANDOM, chave = "randomkey-3", clienteId = CLIENTE_ID))
    }

    @Test
    internal fun `deve listar todas as chaves do cliente`() {
        val clienteId = CLIENTE_ID.toString()

        val response = grpcClient.lista(
            ListaChavesPixRequest.newBuilder()
                .setClientId(clienteId)
                .build()
        )
        with(response.chavesList) {
            assertThat(this, hasSize(2))
            assertThat(
                this.map { Pair(it.tipo, it.chave) }.toList(),
                containsInAnyOrder(
                    Pair(br.com.zup.edu.tarcio.TipoDeChave.RANDOM, "randomkey-3"),
                    Pair(br.com.zup.edu.tarcio.TipoDeChave.EMAIL, "rafael.ponte@zup.com.br")
                )
            )
        }

    }

    @Test
    fun `nao deve listar as chaves do cliente quando cliente nao possuir chaves`() {
        // cenário
        val clienteSemChaves = UUID.randomUUID().toString()

        // ação
        val response = grpcClient.lista(
            ListaChavesPixRequest.newBuilder()
                .setClientId(clienteSemChaves)
                .build()
        )

        // validação
        assertEquals(0, response.chavesCount)
    }

    @Test
    fun `nao deve listar todas as chaves do cliente quando clienteId for invalido`() {
        // cenário
        val clienteIdInvalido = ""

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.lista(
                ListaChavesPixRequest.newBuilder()
                    .setClientId(clienteIdInvalido)
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Cliente ID não pode ser nulo ou vazio", status.description)
        }
    }

    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeyManagerListaServiceGrpc.KeyManagerListaServiceBlockingStub? {
            return KeyManagerListaServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun chave(
        tipo: TipoDeChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = UUID.randomUUID(),
    ): ChavePix {
        return ChavePix(
            clientId = clienteId,
            tipo = tipo,
            chave = chave,
            tipoDeConta = TipoDeConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                instituicao = "UNIBANCO ITAU",
                nomeDoTitular = "Rafael Ponte",
                cpfDoTitular = "12345678900",
                agencia = "1218",
                numeroDaConta = "123456"
            )
        )
    }

}