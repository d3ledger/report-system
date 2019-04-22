package com.d3.report.controllers

import com.d3.report.repository.TransferAssetRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/report")
class ReportController {

    @Autowired
    lateinit var transaferRepo: TransferAssetRepo


    @GetMapping("/billing/transferAsset?from={from}&to={to}&pageNum={pageNum}")
    fun reportBillingtransferAsset(
        @RequestParam("from") from:Long,
        @RequestParam("to") to:Long,
        @PathVariable pageNum:Int
        ) {
        /**
         * 1. Here we should get all transferAssets.
         * 2. Then find transfers from same transactions
         * 3. Check that there are one transfer with commission.
         */
    }


    @GetMapping("/billing/transferAsset/summary?from={from}&to={to}")
    fun reportBillingtransferAssetSummary(
        @RequestParam("from") from:Long,
        @RequestParam("to") to:Long
    ) {
        /**
         * 1. Here we should get all transferAssets.
         * 2. Then find transfers from same transactions
         * 3. Check that there are one transfer with commission.
         */
    }
}
