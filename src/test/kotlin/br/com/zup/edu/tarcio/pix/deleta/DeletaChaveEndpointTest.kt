package br.com.zup.edu.tarcio.pix.deleta

import br.com.zup.edu.tarcio.DeletaChavePixRequest
import br.com.zup.edu.tarcio.KeyManagerDeletaServiceGrpc
import br.com.zup.edu.tarcio.TipoDeConta
import br.com.zup.edu.tarcio.pix.ChavePix
import br.com.zup.edu.tarcio.pix.ChavePixRepository
import br.com.zup.edu.tarcio.pix.ContaAssociada
import br.com.zup.edu.tarcio.pix.TipoChave
import org.junit.jupiter.api.Assertions.*

import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import javax.inject.Singleton


@MicronautTest(transactional = false)
internal class DeletaChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerDeletaServiceGrpc.KeyManagerDeletaServiceBlockingStub
) {

    lateinit var  existente : ChavePix

    @BeforeEach
    internal fun setUp() {
         existente = repository.save(
            ChavePix(
                clientId = CLIENT_ID,
                tipo = TipoChave.EMAIL,
                chave = "email@teste.com",
                tipoDeConta = TipoDeConta.CONTA_CORRENTE,
                conta = ContaAssociada(
                    instituicao = "ITAÚ UNIBANCO S.A",
                    nomeDoTitular = "Rafael M C Ponte",
                    cpfDoTitular = "02467781054",
                    agencia = "0001",
                    numeroDaConta = "291900",
                    ispb = "60701190"
                )
            )
        )
    }

    @AfterEach
    fun cleanUp(){
        repository.deleteAll()
    }

    companion object {
        val CLIENT_ID = UUID.randomUUID()
        val PIX_ID = UUID.randomUUID()

    }

    @Test
    internal fun `deve deletar uma chave pix existente`() {

        val request = DeletaChavePixRequest.newBuilder()
            .setClientId(existente.clientId.toString())
            .setPixId(existente.pixId.toString())
            .build()

        val response = grpcClient.deleta(request)

        assertEquals(CLIENT_ID.toString(), response.clientId.toString())
        assertFalse(repository.existsByChave(existente.pixId.toString()))
    }

    @Test
    internal fun `deve lancar excecao quando a chave nao existir`() {

        val chavePixInexistente = UUID.randomUUID().toString()

        val request = DeletaChavePixRequest.newBuilder()
            .setClientId(CLIENT_ID.toString())
            .setPixId(chavePixInexistente)
            .build()


        val error = assertThrows<StatusRuntimeException> {
            grpcClient.deleta(request)
        }

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix '${chavePixInexistente}' não existe", status.description)
        }
    }

    @Test
    internal fun `deve lancar excecao quando nao for o dono da chave que tenta excluir a chave`() {

        val outroClientId = UUID.randomUUID().toString()

        val request = DeletaChavePixRequest.newBuilder()
            .setClientId(outroClientId)
            .setPixId(existente.pixId.toString())
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.deleta(request)
        }

        with(error) {
            assertEquals(Status.PERMISSION_DENIED.code, status.code)
            assertEquals("Cliente não tem permissão para apagar essa chave", status.description)
        }
    }

    @Factory
    class Deleta {
        @Singleton
        fun blockingStub(
            @GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel
        ): KeyManagerDeletaServiceGrpc.KeyManagerDeletaServiceBlockingStub {
            return KeyManagerDeletaServiceGrpc.newBlockingStub(channel)
        }
    }
}