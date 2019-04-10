package jp.co.soramitsu.d3.datacollector.utils


fun getDomainFromAccountId(accountId:String) : String = accountId.substring(accountId.indexOf('@') + 1)