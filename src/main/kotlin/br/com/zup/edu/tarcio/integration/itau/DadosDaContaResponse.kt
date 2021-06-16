package br.com.zup.edu.tarcio.integration.itau

import br.com.zup.edu.tarcio.pix.ContaAssociada

data class DadosDaContaResponse(
    val tipo: String,
    val instituicao: InstituicaoResponse,
    val agencia: String,
    val numero: String,
    val titular: TitularResponse
) {
    fun toModel(): ContaAssociada {
        return ContaAssociada(
            instituicao = this.instituicao.nome,
            nomeDoTitular = this.titular.nome,
            cpfDoTitular = this.titular.cpf,
            agencia = this.agencia,
            numeroDaCota = this.numero,
        )
    }
}

data class InstituicaoResponse(
    val nome: String,
    val ispb: String
)

data class TitularResponse(
    val nome: String,
    val cpf: String
)