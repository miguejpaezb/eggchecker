package com.adso.eggchecker.data.remote

import com.adso.eggchecker.BuildConfig
import com.adso.eggchecker.data.local.SessionDataStore

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Construye la instancia de Retrofit que consume la API de EggChecker. */
object ApiClient {

    /**
     * Crea el servicio HTTP con el interceptor de autenticación.
     *
     * @param sessionDataStore Fuente del token para el encabezado Bearer.
     * @return ApiService listo para usarse.
     */
    fun crear(sessionDataStore: SessionDataStore): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionDataStore))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
