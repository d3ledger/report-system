package com.d3.datacollector.controllers

import com.d3.datacollector.model.Conflictable

enum class DcExceptionStatus {
    ASSET_NOT_FOUND,
    FEE_NOT_SET,
    UNKNOWN_ERROR
}

fun Conflictable.fill(status: DcExceptionStatus, exception: Exception) {
    this.errorCode = status.name
    this.message = "${exception.javaClass.simpleName}: ${exception.message}"
}
