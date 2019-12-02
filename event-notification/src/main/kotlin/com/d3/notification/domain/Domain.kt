package com.d3.notification.domain

import com.d3.notifications.event.SoraECDSASignature
import com.d3.notifications.event.SoraEthWithdrawalProofsEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

private val gson = Gson()

@Entity
@Table(name = "eth_withdrawal_proof")
data class EthWithdrawalProofs(
    @Id
    val id: String = "",
    val accountIdToNotify: String = "",
    val tokenContractAddress: String = "",
    val amount: String = "",
    val relay: String = "",
    val proofsJson: String,
    val irohaTxHash: String = "",
    val destAddress: String = "",
    val txTime: Long = 0,
    val blockNum: Long = 0,
    val txIndex: Int = 0
) {

    /**
     * Returns proofs as a list
     * @return list of proofs
     */
    fun getProofs(): List<SoraECDSASignature> {
        val listType = object : TypeToken<List<SoraECDSASignature>>() {}.type
        return gson.fromJson(this.proofsJson, listType)
    }

    /**
     * Maps domain object to event
     * @return mapped event object
     */
    fun mapToEvent(): SoraEthWithdrawalProofsEvent {
        return SoraEthWithdrawalProofsEvent(
            id = this.id,
            accountIdToNotify = this.accountIdToNotify,
            tokenContractAddress = this.tokenContractAddress,
            relay = this.relay,
            irohaTxHash = this.irohaTxHash,
            to = this.destAddress,
            txTime = this.txTime,
            amount = BigDecimal(this.amount),
            proofs = this.getProofs(),
            blockNum = this.blockNum,
            txIndex = this.txIndex
        )
    }

    companion object {
        /**
         * Maps event object to domain
         * @param soraEthWithdrawalProofsEvent - event object to map
         * @return mapped domain object
         */
        fun mapDomain(soraEthWithdrawalProofsEvent: SoraEthWithdrawalProofsEvent): EthWithdrawalProofs {
            return EthWithdrawalProofs(
                id = soraEthWithdrawalProofsEvent.id,
                accountIdToNotify = soraEthWithdrawalProofsEvent.accountIdToNotify,
                tokenContractAddress = soraEthWithdrawalProofsEvent.tokenContractAddress,
                amount = soraEthWithdrawalProofsEvent.amount.toPlainString(),
                relay = soraEthWithdrawalProofsEvent.relay,
                irohaTxHash = soraEthWithdrawalProofsEvent.irohaTxHash,
                destAddress = soraEthWithdrawalProofsEvent.to,
                txTime = soraEthWithdrawalProofsEvent.txTime,
                proofsJson = gson.toJson(soraEthWithdrawalProofsEvent.proofs),
                blockNum = soraEthWithdrawalProofsEvent.blockNum,
                txIndex = soraEthWithdrawalProofsEvent.txIndex
            )
        }
    }
}
