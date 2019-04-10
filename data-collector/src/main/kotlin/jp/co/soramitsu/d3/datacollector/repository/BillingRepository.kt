package jp.co.soramitsu.d3.datacollector.repository

import jp.co.soramitsu.d3.datacollector.model.Billing
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.math.BigDecimal
import java.util.*

interface BillingRepository : CrudRepository<Billing, Long> {

/*    @Modifying
    @Query("update Billing b set b.feeFraction = ?1 where b.accountId = ?2 and b.asset = ?3 and b.billingType = ?4")
    fun updateByAccountIdAndBillingTypeAndAsset(
        feeFraction: BigDecimal,
        accountId: String,
        asset: String,
        billingType: Billing.BillingTypeEnum
    ): Int*/

    @Query("SELECT b FROM Billing b WHERE b.accountId = ?1 and b.asset = ?2 and b.billingType = ?3")
    fun selectByAccountIdBillingTypeAndAsset(
        accountId: String,
        asset: String,
        billingType: Billing.BillingTypeEnum
    ): Optional<Billing>
}