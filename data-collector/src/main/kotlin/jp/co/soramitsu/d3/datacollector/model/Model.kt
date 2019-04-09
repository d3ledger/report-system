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