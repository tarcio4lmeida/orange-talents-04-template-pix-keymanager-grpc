package br.com.zup.edu.tarcio.shared.grpc.handlers

import br.com.zup.edu.tarcio.shared.grpc.ExceptionHandler
import io.grpc.Status

/**
 * By design, this class must NOT be managed by Micronaut
 */
class DefaultExceptionHandler : ExceptionHandler<Exception> {

    override fun handle(e: Exception): ExceptionHandler.StatusWithDetails {
        val status = when (e) {
            is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message)
            is IllegalStateException -> Status.FAILED_PRECONDITION.withDescription(e.message)
            else -> Status.UNKNOWN
        }
        return ExceptionHandler.StatusWithDetails(status.withCause(e))
    }

    override fun supports(e: Exception): Boolean {
        return true
    }

}