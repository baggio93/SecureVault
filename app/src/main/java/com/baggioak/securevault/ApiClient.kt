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
import java.util.concurrent.TimeUnit // AGGIUNGI QUESTO IMPORT
import javax.net.ssl.*

object ApiClient {

    private var retrofit: Retrofit? = null

    // FUNZIONE AGGIUNTA: Serve a "pulire" il client se cambiamo IP
    fun resetClient() {
        retrofit = null
    }

    fun getApiService(context: Context): ApiService {
        if (retrofit == null) {
            val serverIp = SettingsManager.getServerIp(context)

            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            val cookieManager = CookieManager()
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)

            // MODIFICA QUI: Aggiungiamo i timeout al builder
            val client = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS) // Aspetta max 3s per connettersi
                .readTimeout(3, TimeUnit.SECONDS)    // Aspetta max 3s per leggere i dati
                .writeTimeout(3, TimeUnit.SECONDS)   // Aspetta max 3s per inviare i dati
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