package br.com.zup.edu.tarcio.shared.grpc

import br.com.zup.edu.tarcio.pix.ChavePixExistenteException
import br.com.zup.edu.tarcio.pix.ChavePixInexistenteException
import br.com.zup.edu.tarcio.pix.PermissaoNegadaException
import io.grpc.Status
import javax.validation.ConstraintViolationException

class DefaultExceptionHandler : ExceptionHandler<Exception> {

    override fun handle(e: Exception): ExceptionHandler.StatusWithDetails {
        val status = when(e) {
            is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message)
            is IllegalStateException -> Status.FAILED_PRECONDITION.withDescription(e.message)
            is ConstraintViolationException -> Status.INVALID_ARGUMENT.withDescription(e.message)
            is ChavePixExistenteException -> Status.ALREADY_EXISTS.withDescription(e.message)
            is ChavePixInexistenteException -> Status.NOT_FOUND.withDescription(e.message)
            is PermissaoNegadaException -> Status.PERMISSION_DENIED.withDescription(e.message)
            else -> Status.UNKNOWN.withDescription(e.message)
        }
        return ExceptionHandler.StatusWithDetails(status.withCause(e))
    }

    override fun supports(e: Exception): Boolean {
        return true
    }
}