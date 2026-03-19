package com.perdonus.r34viewer.data.remote

import com.perdonus.r34viewer.BuildConfig
import com.perdonus.r34viewer.data.settings.AppSettings
import com.perdonus.r34viewer.data.settings.ProxyConfig
import com.perdonus.r34viewer.data.settings.ProxyType
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.util.concurrent.TimeUnit

class NetworkClientFactory {
    fun create(settings: AppSettings): OkHttpClient {
        val proxy = settings.proxyConfig.toJavaProxy()
        installSocksAuthenticator(settings.proxyConfig)

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .proxy(proxy)
            .proxyAuthenticator { _, response ->
                val config = settings.proxyConfig
                if (
                    !config.enabled ||
                    config.type != ProxyType.HTTP ||
                    !config.hasCredentials ||
                    response.request.header("Proxy-Authorization") != null
                ) {
                    return@proxyAuthenticator null
                }

                response.request.newBuilder()
                    .header("Proxy-Authorization", Credentials.basic(config.username, config.password))
                    .build()
            }
            .addInterceptor(logging)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "R34NativeAndroid/${BuildConfig.VERSION_NAME}")
                        .build(),
                )
            }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun installSocksAuthenticator(config: ProxyConfig) {
        if (config.enabled && config.type == ProxyType.SOCKS && config.hasCredentials) {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(config.username, config.password.toCharArray())
                }
            })
        } else {
            Authenticator.setDefault(null)
        }
    }

    private fun ProxyConfig.toJavaProxy(): Proxy? {
        if (!enabled || host.isBlank() || port == null) return null
        val address = InetSocketAddress(host, port)
        return when (type) {
            ProxyType.HTTP -> Proxy(Proxy.Type.HTTP, address)
            ProxyType.SOCKS -> Proxy(Proxy.Type.SOCKS, address)
        }
    }
}
