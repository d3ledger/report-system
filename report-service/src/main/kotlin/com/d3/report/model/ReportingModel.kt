package com.d3.report.model

import com.d3.report.dto.Conflict

class Transfer(
    var transfer: TransferAsset? = null,
    var fee: TransferAsset? = null
)

open class BaseReport(
    var total:Long,
    var pages:Int,
    override val errorCode: String?,
    override val errorMessage: String?
) : Conflict

class TransferReport(
    val transfers: ArrayList<Transfer> = ArrayList(),
    total:Long = 0,
    pages:Int = 0,
    code: String? = null,
    message: String? = null
): BaseReport(total,pages, code, message)
