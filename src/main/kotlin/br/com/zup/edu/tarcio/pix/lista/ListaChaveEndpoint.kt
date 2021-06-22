package br.com.zup.edu.tarcio.pix.lista

import br.com.zup.edu.tarcio.KeyManagerListaServiceGrpc
import br.com.zup.edu.tarcio.ListaChavesPixRequest
import br.com.zup.edu.tarcio.ListaChavesPixResponse
import br.com.zup.edu.tarcio.*
import br.com.zup.edu.tarcio.pix.ChavePixRepository
import br.com.zup.edu.tarcio.shared.grpc.ErrorHandler
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import java.lang.IllegalArgumentException
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class ListaChaveEndpoint(@Inject private val repository: ChavePixRepository) :
    KeyManagerListaServiceGrpc.KeyManagerListaServiceImplBase() {

    override fun lista(
        request: ListaChavesPixRequest,
        responseObserver: StreamObserver<ListaChavesPixResponse>
    ) {
        if (request.clientId.isNullOrBlank())
            throw IllegalArgumentException("Cliente ID não pode ser nulo ou vazio")

        val clientId = UUID.fromString(request.clientId)
        val chaves = repository.findAllByClientId(clientId).map {
            ListaChavesPixResponse.ChavePix.newBuilder()
                .setPixId(it.pixId.toString())
                .setTipo(TipoDeChave.valueOf(it.tipo.name))
                .setChave(it.chave)
                .setTipoDeConta(TipoDeConta.valueOf(it.tipoDeConta.name))
                .setCriadaEm(it.criadaEm.let {
                    val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder()
                        .setSeconds(createdAt.epochSecond)
                        .setNanos(createdAt.nano)
                        .build()
                })
                .build()
        }

        responseObserver.onNext(
            ListaChavesPixResponse.newBuilder() // 1
                .setClientId(clientId.toString())
                .addAllChaves(chaves)
                .build()
        )
        responseObserver.onCompleted()

    }
}