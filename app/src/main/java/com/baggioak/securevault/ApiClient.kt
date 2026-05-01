package com.baggioak.securevault

import android.content.Context
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

/**
 * Gestisce la connessione di rete e fornisce l'istanza del servizio API.
 */
object ApiClient {

    private var retrofit: Retrofit? = null

    /**
     * Resetta il client corrente. Indispensabile quando si cambia l'IP del server
     * o il timeout, per forzare la creazione di una nuova connessione.
     */
    fun resetClient() {
        retrofit = null
    }

    /**
     * Costruisce e restituisce il servizio API configurato con l'IP e il timeout
     * attualmente salvati nelle impostazioni dell'app.
     *
     * @param context Il contesto necessario per leggere le impostazioni.
     * @return L'interfaccia ApiService pronta all'uso.
     */
    fun getApiService(context: Context): ApiService {
        if (retrofit == null) {
            val serverIp = SettingsManager.getServerIp(context)

            // 1. RECUPERIAMO IL TIMEOUT DALLE IMPOSTAZIONI
            val timeoutMillis = SettingsManager.getTimeoutMillis(context)

            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            val cookieManager = CookieManager()
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)

            // 2. USIAMO LA VARIABILE NEI TIMEOUT
            val client = OkHttpClient.Builder()
                .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .cookieJar(JavaNetCookieJar(cookieManager))
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl("https://$serverIp/pw-manager/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}