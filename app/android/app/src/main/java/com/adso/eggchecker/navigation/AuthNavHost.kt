package com.adso.eggchecker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.adso.eggchecker.ui.auth.LoginScreen
import com.adso.eggchecker.ui.auth.LoginViewModel
import com.adso.eggchecker.ui.auth.RecoverScreen
import com.adso.eggchecker.ui.auth.RegisterScreen
import com.adso.eggchecker.ui.auth.RegisterViewModel

private const val RUTA_LOGIN = "login"
private const val RUTA_REGISTER = "register"
private const val RUTA_RECOVER = "recover"

/**
 * Flujo de autenticación: login, registro y recuperación.
 * Solo se muestra cuando no hay una sesión vigente.
 */
@Composable
fun AuthNavHost(factory: ViewModelProvider.Factory) {
    val navController = rememberNavController()
    var correoRegistrado by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf("") }

    NavHost(navController = navController, startDestination = RUTA_LOGIN) {
        composable(RUTA_LOGIN) {
            val loginViewModel: LoginViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = loginViewModel,
                correoInicial = correoRegistrado,
                mensajeExito = mensajeExito,
                onIrARegistro = {
                    correoRegistrado = ""
                    mensajeExito = ""
                    navController.navigate(RUTA_REGISTER)
                },
                onIrARecuperar = { navController.navigate(RUTA_RECOVER) }
            )
        }
        composable(RUTA_REGISTER) {
            val registerViewModel: RegisterViewModel = viewModel(factory = factory)
            RegisterScreen(
                viewModel = registerViewModel,
                onRegistrado = { correo ->
                    correoRegistrado = correo
                    mensajeExito =
                        "Cuenta creada con éxito. Inicia sesión para continuar."
                    navController.popBackStack(RUTA_LOGIN, inclusive = false)
                },
                onIrALogin = {
                    navController.popBackStack(RUTA_LOGIN, inclusive = false)
                }
            )
        }
        composable(RUTA_RECOVER) {
            RecoverScreen(onIrALogin = { navController.popBackStack() })
        }
    }
}
