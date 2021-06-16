package br.com.zup.edu.tarcio.pix.deleta

import br.com.zup.edu.tarcio.DeletaChavePixRequest
import br.com.zup.edu.tarcio.DeletaChavePixResponse
import br.com.zup.edu.tarcio.KeyManagerDeletaServiceGrpc
import br.com.zup.edu.tarcio.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class DeletaChaveEndpoint(
    @Inject private val service: RemoveChaveService
) :
    KeyManagerDeletaServiceGrpc.KeyManagerDeletaServiceImplBase() {

    private val logger = LoggerFactory.getLogger((this::class.java))

    override fun deleta(
        request: DeletaChavePixRequest,
        responseObserver: StreamObserver<DeletaChavePixResponse>
    ) {

        service.remove(request.clientId, request.pixId)

        responseObserver.onNext(
            DeletaChavePixResponse.newBuilder()
                .setClientId(request.clientId)
                .setPixId(request.pixId)
                .build()
        )
        responseObserver.onCompleted()
    }
}