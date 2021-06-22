package br.com.zup.edu.tarcio.pix.deleta

import br.com.zup.edu.tarcio.DeletaChavePixRequest
import br.com.zup.edu.tarcio.KeyManagerDeletaServiceGrpc
import br.com.zup.edu.tarcio.integration.bcb.BcbClient
import br.com.zup.edu.tarcio.integration.bcb.DeletePixKeyRequest
import br.com.zup.edu.tarcio.integration.bcb.DeletePixKeyResponse
import br.com.zup.edu.tarcio.pix.*
import org.junit.jupiter.api.Assertions.*

import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@MicronautTest(transactional = false)
internal class DeletaChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerDeletaServiceGrpc.KeyManagerDeletaServiceBlockingStub
) {

    lateinit var  existente : ChavePix

    @field:Inject
    lateinit var bcbClient: BcbClient



    @BeforeEach
    internal fun setUp() {
         existente = repository.save(
            chave(
                tipo = TipoDeChave.EMAIL,
                chave = "rponte@gmail.com",
                clienteId = UUID.randomUUID()
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

        val bcbResponse = DeletePixKeyResponse(
            key = "email@teste.com",
            participant = "60701190",
            deletedAt = LocalDateTime.now()
        )
        `when`(
            bcbClient.deletaChavePixBcb(
                DeletePixKeyRequest(
                    key = "rponte@gmail.com"
                ), "rponte@gmail.com"
            )
        )
            .thenReturn(HttpResponse.ok(bcbResponse))

        val response = grpcClient.deleta(request)

        assertEquals(existente.clientId.toString(), response.clientId.toString())
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

    @Test
    internal fun `deve lancar excecao quando possivel remover chave no bcb`() {

        val request = DeletaChavePixRequest.newBuilder()
            .setClientId(existente.clientId.toString())
            .setPixId(existente.pixId.toString())
            .build()

        `when`(
            bcbClient.deletaChavePixBcb(
                DeletePixKeyRequest(
                    key = "rponte@gmail.com"
                ), "rponte@gmail.com"
            )
        )
            .thenReturn(HttpResponse.unprocessableEntity())


        val error = assertThrows<StatusRuntimeException> {
            grpcClient.deleta(request)
        }

        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Falha na remoção de chave no BCB", status.description)
        }
    }

    @MockBean(BcbClient::class)
    fun bcbClientMock(): BcbClient {
        return mock(BcbClient::class.java)
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