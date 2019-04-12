package com.d3.datacollector.utils


fun getDomainFromAccountId(accountId:String) : String = accountId.split("@")[1]
