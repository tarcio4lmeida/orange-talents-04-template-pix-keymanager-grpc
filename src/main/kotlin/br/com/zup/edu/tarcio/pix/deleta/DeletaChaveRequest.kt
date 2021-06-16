package br.com.zup.edu.tarcio.pix.deleta

import br.com.zup.edu.tarcio.shared.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Introspected
data class DeletaChaveRequest(
    @ValidUUID
    @field:NotBlank
    val clientId: UUID,

    @field:NotNull
    val pixId: Long,
)