package br.com.zup.edu.tarcio.pix.register

import br.com.zup.edu.tarcio.KeyManagerRegistraServiceGrpc
import br.com.zup.edu.tarcio.RegistraChavePixRequest
import br.com.zup.edu.tarcio.TipoDeChave
import br.com.zup.edu.tarcio.TipoDeConta
import br.com.zup.edu.tarcio.integration.bcb.*
import br.com.zup.edu.tarcio.integration.itau.DadosDaContaResponse
import br.com.zup.edu.tarcio.integration.itau.InstituicaoResponse
import br.com.zup.edu.tarcio.integration.itau.ItauClient
import br.com.zup.edu.tarcio.integration.itau.TitularResponse
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
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class RegistraChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerRegistraServiceGrpc.KeyManagerRegistraServiceBlockingStub,
) {
    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @BeforeEach
    internal fun setUp() {
        repository.deleteAll()
    }

    @field:Inject
    lateinit var bcbClient: BcbClient

    @field:Inject
    lateinit var itauClient: ItauClient

    @Test
    internal fun `deve cadastrar uma nova chave pix`() {

        `when`(itauClient.buscaContaPorTipo(CLIENTE_ID.toString(), "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChaveBcb(createPixKeyRequest()))
            .thenReturn(HttpResponse.created(createPixKeyResponse()))

        val response = grpcClient.registra(RegistraChavePixRequest.newBuilder()
            .setClientId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.EMAIL)
            .setChave("teste@email.com")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build())

        with(response) {
            assertEquals(CLIENTE_ID.toString(), clientId)
            assertNotNull(pixId)

        }
    }

    @Test
    internal fun `nao deve cadastrar uma nova chave pix com chave ja existente`() {
        val existente = repository.save(
            ChavePix(
                clientId = UUID.fromString("c56dfef4-7901-44fb-84e2-a2cefb157890"),
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

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(RegistraChavePixRequest.newBuilder()
                .setClientId(existente.clientId.toString())
                .setTipoDeChave(TipoDeChave.EMAIL)
                .setTipoDeConta(existente.tipoDeConta)
                .setChave(existente.chave)
                .build())
        }

        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Chave Pix '${existente.chave}' existente", status.description)
        }

    }
    @Test
    internal fun `nao deve cadastrar uma nova chave pix quando o cliente nao e encontrado no itau`() {
        val request = RegistraChavePixRequest.newBuilder()
            .setClientId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.EMAIL)
            .setChave("teste@email.com")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        `when`(itauClient.buscaContaPorTipo(request.clientId, request.tipoDeConta.name))
            .thenReturn(HttpResponse.notFound())

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Cliente não encontrado no Itaú", status.description)
        }
    }

    @Test
    internal fun `nao deve cadastrar uma nova chave pix com dados invalidos`() {
        //vazio
        val request = RegistraChavePixRequest.newBuilder()
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertTrue(status.description!!.split(",").contains(" registra.novaChave: chave Pix invalida"))
            println(status.description!!.split(","))
        }
    }

    private fun createPixKeyRequest(): CreatePixKeyRequest {
        return CreatePixKeyRequest(
            keyType = "EMAIL",
            key = "teste@email.com",
            bankAccount = BankAccout(
                participant = "60701190",
                branch = "1218",
                accountNumber = "291900",
                accountType = AccountType.CACC
            ),
            owner = Owner(
                type = OwnerType.NATURAL_PERSON,
                name = "Rafael Ponte",
                taxIdNumber = "63657520325"
            )
        )
    }

    private fun createPixKeyResponse() : CreatePixKeyResponse {
        return CreatePixKeyResponse(
            keyType = "EMAIL",
            key = "teste@email.com",
            bankAccount = BankAccout(
                participant = "60701190",
                branch = "0001",
                accountNumber = "291900",
                accountType = AccountType.CACC
            ),
            owner = Owner(
                type = OwnerType.NATURAL_PERSON,
                name = "Rafael M C Ponte",
                taxIdNumber = "02467781054"
            ),
            createdAt = LocalDateTime.now()
        )
    }

    private fun dadosDaContaResponse(): DadosDaContaResponse {
        return DadosDaContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoResponse("UNIBANCO ITAU SA", "60701190"),
            agencia = "1218",
            numero = "291900",
            titular = TitularResponse("Rafael Ponte", "63657520325")
        )
    }

    @MockBean(ItauClient::class)
    fun itauClientMock(): ItauClient {
        return mock(ItauClient::class.java)
    }

    @MockBean(BcbClient::class)
    fun bcbClientMock(): BcbClient {
        return mock(BcbClient::class.java)
    }

    @Factory
    class Registra {
        @Singleton
        fun blockingStub(
            @GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel
        ): KeyManagerRegistraServiceGrpc.KeyManagerRegistraServiceBlockingStub {
            return KeyManagerRegistraServiceGrpc.newBlockingStub(channel)
        }
    }
}

