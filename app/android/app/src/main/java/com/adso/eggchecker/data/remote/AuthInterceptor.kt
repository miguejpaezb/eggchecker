package com.adso.eggchecker.data.remote

import com.adso.eggchecker.data.local.SessionDataStore

import kotlinx.coroutines.runBlocking

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Añade el encabezado Authorization a cada petición cuando hay sesión.
 * El backend EggChecker espera `Bearer <jwt>`.
 */
class AuthInterceptor(
    private val sessionDataStore: SessionDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { sessionDataStore.tokenActual() }
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
