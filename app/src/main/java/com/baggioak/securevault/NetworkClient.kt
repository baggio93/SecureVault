package com.baggioak.securevault

import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object NetworkClient {

    // Abbiamo aggiunto "serverIp: String" tra le parentesi
    fun getUnsafeOkHttpClient(serverIp: String): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)

            // Qui ora usiamo l'IP dinamico passato dal bottone delle impostazioni
            builder.hostnameVerifier { hostname, _ ->
                hostname == serverIp
            }

            return builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}