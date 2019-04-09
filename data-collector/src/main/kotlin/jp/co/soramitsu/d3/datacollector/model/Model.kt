package jp.co.soramitsu.d3.datacollector.model

import javax.persistence.*

@Entity
data class State(
    @Id @GeneratedValue
    val id: Long = 0,
    var title: String = "",
    var value: String = ""
) {
    enum class StateEnum {
        DEFAULT,
        LAST_PROCESSED_BLOCK_ID,
    }
}