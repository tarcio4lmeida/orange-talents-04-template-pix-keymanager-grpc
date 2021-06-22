package br.com.zup.edu.tarcio.pix.carrega

import br.com.zup.edu.tarcio.util.violations
import br.com.zup.edu.tarcio.CarregaChavePixRequest
import br.com.zup.edu.tarcio.KeyManagerCarregaServiceGrpc
import br.com.zup.edu.tarcio.integration.bcb.BankAccount
import br.com.zup.edu.tarcio.integration.bcb.BcbClient
import br.com.zup.edu.tarcio.integration.bcb.Owner
import br.com.zup.edu.tarcio.integration.bcb.PixKeyDetailsResponse
import br.com.zup.edu.tarcio.integration.bcb.PixKeyType.*
import br.com.zup.edu.tarcio.pix.*

import org.junit.jupiter.api.Assertions.*

import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

/**
 * TIP: Microunaut ainda não suporta @Nested classes do jUnit5
 *  - https://github.com/micronaut-projects/micronaut-test/issues/56
 */
@MicronautTest(transactional = false)
internal class CarregaChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerCarregaServiceGrpc.KeyManagerCarregaServiceBlockingStub,
) {

    @Inject
    lateinit var bcbClient: BcbClient

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    /**
     * TIP: por padrão roda numa transação isolada
     */
    @BeforeEach
    fun setup() {
        repository.save(chave(tipo = TipoDeChave.EMAIL, chave = "rafael.ponte@zup.com.br", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = TipoDeChave.CPF, chave = "63657520325", clienteId = UUID.randomUUID()))
        repository.save(chave(tipo = TipoDeChave.RANDOM, chave = "randomkey-3", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = TipoDeChave.PHONE, chave = "+551155554321", clienteId = CLIENTE_ID))
    }

    /**
     * TIP: por padrão roda numa transação isolada
     */
    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve carregar chave por pixId e clienteId`() {
        // cenário
        val chaveExistente = repository.findByChave("+551155554321").get()

        // ação
        val response = grpcClient.carrega(
            CarregaChavePixRequest.newBuilder()
                .setPixId(
                    CarregaChavePixRequest.FiltroPorPixId.newBuilder()
                        .setPixId(chaveExistente.pixId.toString())
                        .setClientId(chaveExistente.clientId.toString())
                        .build()
                ).build()
        )

        // validação
        with(response) {
            assertEquals(chaveExistente.pixId.toString(), this.pixId)
            assertEquals(chaveExistente.clientId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipo.name, this.chave.tipo.name)
            assertEquals(chaveExistente.chave, this.chave.chave)
        }
    }

    @Test
    fun `nao deve carregar chave por pixId e clienteId quando registro nao existir`() {
        // ação
        val pixIdNaoExistente = UUID.randomUUID().toString()
        val clienteIdNaoExistente = UUID.randomUUID().toString()
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(
                CarregaChavePixRequest.newBuilder()
                    .setPixId(
                        CarregaChavePixRequest.FiltroPorPixId.newBuilder()
                            .setPixId(pixIdNaoExistente)
                            .setClientId(clienteIdNaoExistente)
                            .build()
                    ).build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `deve carregar chave por valor da chave quando registro existir localmente`() {
        // cenário
        val chaveExistente = repository.findByChave("rafael.ponte@zup.com.br").get()

        // ação
        val response = grpcClient.carrega(
            CarregaChavePixRequest.newBuilder()
                .setChave("rafael.ponte@zup.com.br")
                .build()
        )

        // validação
        with(response) {
            assertEquals(chaveExistente.pixId.toString(), this.pixId)
            assertEquals(chaveExistente.clientId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipo.name, this.chave.tipo.name)
            assertEquals(chaveExistente.chave, this.chave.chave)
        }
    }

    @Test
    fun `deve carregar chave por valor da chave quando registro nao existir localmente mas existir no BCB`() {
        // cenário
        val bcbResponse = pixKeyDetailsResponse()
        `when`(bcbClient.buscaChavePixBcb(key = "user.from.another.bank@santander.com.br"))
            .thenReturn(HttpResponse.ok(pixKeyDetailsResponse()))

        // ação
        val response = grpcClient.carrega(
            CarregaChavePixRequest.newBuilder()
                .setChave("user.from.another.bank@santander.com.br")
                .build()
        )

        // validação
        with(response) {
            assertEquals("", this.pixId)
            assertEquals("", this.clienteId)
            assertEquals(bcbResponse.keyType.name, this.chave.tipo.name)
            assertEquals(bcbResponse.key, this.chave.chave)
        }
    }

    @Test
    fun `nao deve carregar chave por valor da chave quando registro nao existir localmente nem no BCB`() {
        // cenário
        `when`(bcbClient.buscaChavePixBcb(key = "not.existing.user@santander.com.br"))
            .thenReturn(HttpResponse.notFound())

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(
                CarregaChavePixRequest.newBuilder()
                    .setChave("not.existing.user@santander.com.br")
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `nao deve carregar chave por pixId e clienteId quando filtro invalido`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(
                CarregaChavePixRequest.newBuilder()
                    .setPixId(
                        CarregaChavePixRequest.FiltroPorPixId.newBuilder()
                            .setPixId("")
                            .setClientId("")
                            .build()
                    ).build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            assertThat(
                violations(), containsInAnyOrder(
                    Pair("pixId", "não deve estar em branco"),
                    Pair("clientId", "não deve estar em branco"),
                    Pair("pixId", "não é um formato válido de UUID"),
                    Pair("clientId", "não é um formato válido de UUID"),
                )
            )
        }
    }

    @Test
    fun `nao deve carregar chave por valor da chave quando filtro invalido`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(CarregaChavePixRequest.newBuilder().setChave("").build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            assertThat(
                violations(), containsInAnyOrder(
                    Pair("chave", "não deve estar em branco"),
                )
            )
        }
    }

    @Test
    fun `nao deve carregar chave quando filtro invalido`() {

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(CarregaChavePixRequest.newBuilder().build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix inválida ou não informada", status.description)
        }
    }


    @MockBean(BcbClient::class)
    fun bcbClient(): BcbClient? {
        return mock(BcbClient::class.java)
    }

    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeyManagerCarregaServiceGrpc.KeyManagerCarregaServiceBlockingStub? {
            return KeyManagerCarregaServiceGrpc.newBlockingStub(channel)
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

    private fun pixKeyDetailsResponse(): PixKeyDetailsResponse {
        return PixKeyDetailsResponse(
            keyType = EMAIL,
            key = "user.from.another.bank@santander.com.br",
            bankAccount = bankAccount(),
            owner = owner(),
            createdAt = LocalDateTime.now()
        )
    }

    private fun bankAccount(): BankAccount {
        return BankAccount(
            participant = "90400888",
            branch = "9871",
            accountNumber = "987654",
            accountType = BankAccount.AccountType.SVGS
        )
    }

    private fun owner(): Owner {
        return Owner(
            type = Owner.OwnerType.NATURAL_PERSON,
            name = "Another User",
            taxIdNumber = "12345678901"
        )
    }
}