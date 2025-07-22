package com.skydevs.tgdrive.utils

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OkHttpClientFactory {
    // 提供一个方法，返回配置好的 OkHttpClient 实例
    @JvmStatic
    fun createClient(): OkHttpClient {
        // 自定义连接池设置
        val connectionPool = ConnectionPool(5, 2, TimeUnit.MINUTES)

        // 创建并返回 OkHttpClient 实例
        return OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}