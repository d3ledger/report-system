package jp.co.soramitsu.d3.datacollector.repository

import jp.co.soramitsu.d3.datacollector.model.State
import org.springframework.data.repository.CrudRepository

interface StateRepository : CrudRepository<State, Long>