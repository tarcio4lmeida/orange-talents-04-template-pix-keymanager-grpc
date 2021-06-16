package br.com.zup.edu.tarcio.pix.register

import br.com.zup.edu.tarcio.*
import br.com.zup.edu.tarcio.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RegistraChaveEndpoint(
    @Inject private val service: NovaChavePixService
) : KeyManagerRegistraServiceGrpc.KeyManagerRegistraServiceImplBase() {

    override fun registra(
        request: RegistraChavePixRequest,
        responseObserver: StreamObserver<RegistraChavePixResponse>
    ) {
        val novaChave = request.toModel()
        val chaveCriada = service.registra(novaChave)

        responseObserver.onNext(
            RegistraChavePixResponse.newBuilder()
                .setClientId(chaveCriada.clientId.toString())
                .setPixId(chaveCriada.pixId.toString())
                .build()
        )
        responseObserver.onCompleted()
    }
}