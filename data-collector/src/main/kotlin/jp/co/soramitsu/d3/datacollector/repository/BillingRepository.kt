package jp.co.soramitsu.d3.datacollector.repository

import jp.co.soramitsu.d3.datacollector.model.Billing
import org.springframework.data.repository.CrudRepository

interface BillingRepository : CrudRepository<Billing, Long>