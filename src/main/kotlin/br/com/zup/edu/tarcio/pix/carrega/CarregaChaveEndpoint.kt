package br.com.zup.edu.tarcio.pix.carrega

import br.com.zup.edu.tarcio.CarregaChavePixRequest
import br.com.zup.edu.tarcio.CarregaChavePixResponse
import br.com.zup.edu.tarcio.integration.bcb.BcbClient
import br.com.zup.edu.tarcio.pix.ChavePixRepository
import br.com.zup.edu.tarcio.KeyManagerCarregaServiceGrpc
import br.com.zup.edu.tarcio.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Validator

@ErrorHandler
@Singleton
class CarregaChaveEndpoint(
    @Inject private val repository: ChavePixRepository, // 1
    @Inject private val bcbClient: BcbClient, // 1
    @Inject private val validator: Validator,
) : KeyManagerCarregaServiceGrpc.KeyManagerCarregaServiceImplBase() {

    override fun carrega(
        request: CarregaChavePixRequest,
        responseObserver: StreamObserver<CarregaChavePixResponse>
    ) {
        val filtro = request.toModel(validator) // 2
        val chaveInfo = filtro.filtra(repository = repository, bcbClient = bcbClient)

        responseObserver.onNext(CarregaChavePixResponseConverter().convert(chaveInfo)) // 1
        responseObserver.onCompleted()

    }
}
