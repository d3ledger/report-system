package jp.co.soramitsu.d3.datacollector.model

import javax.persistence.*

@Entity
@Table(name = "state")
data class State(
    @Id  @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var title: String = "",
    var value: String = ""
) {
    enum class StateEnum {
        DEFAULT,
        LAST_PROCESSED_BLOCK_ID,
    }
}

@Entity
@Table(name = "billing")
data class Billing(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val accountId: String = "",
    @Enumerated(EnumType.STRING)
    val BillingType: BillingTypeEnum = BillingTypeEnum.TRANSFER
) {
    enum class  BillingTypeEnum {
        TRANSFER,
        CUSTODY,
        ACCOUNT_CREATION,
        EXCHANGE,
        WITHDRAWAL
    }
}