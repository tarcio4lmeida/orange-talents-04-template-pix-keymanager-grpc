package br.com.zup.edu.tarcio.pix

import javax.persistence.Embeddable

@Embeddable
class ContaAssociada(
    val instituicao: String,
    val nomeDoTitular: String,
    val cpfDoTitular: String,
    val agencia: String,
    val numeroDaConta: String
) {
    companion object {
        public val ITAU_UNIBANCO_ISPB: String = "60701190"
    }
}