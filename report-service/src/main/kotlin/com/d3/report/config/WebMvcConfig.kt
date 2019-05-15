/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.report.config

import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

class WebMvcConfig {

    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry?) {
                registry!!.addMapping("/**")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedOrigins("*")
                    .allowedHeaders("*")
                    .allowCredentials(true)
            }
        }
    }
}
