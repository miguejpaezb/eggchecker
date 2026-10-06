package com.adso.eggchecker.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.adso.eggchecker.data.sync.RefreshBus
import com.adso.eggchecker.data.sync.VentaPendienteBus
import com.adso.eggchecker.model.MODULOS
import com.adso.eggchecker.navigation.Rutas
import com.adso.eggchecker.ui.camadas.CamadasScreen
import com.adso.eggchecker.ui.camadas.CamadasViewModel
import com.adso.eggchecker.ui.clientes.ClientesScreen
import com.adso.eggchecker.ui.clientes.ClientesViewModel
import com.adso.eggchecker.ui.common.ModulePlaceholderScreen
import com.adso.eggchecker.ui.inventario.InventarioScreen
import com.adso.eggchecker.ui.inventario.InventarioViewModel
import com.adso.eggchecker.ui.produccion.ProduccionScreen
import com.adso.eggchecker.ui.produccion.ProduccionViewModel
import com.adso.eggchecker.ui.shell.components.AppDrawer
import com.adso.eggchecker.ui.shell.components.AppTopbar
import com.adso.eggchecker.ui.shell.components.NotificacionesPanel
import com.adso.eggchecker.ui.shell.components.PerfilMenuCard
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Cream
import com.adso.eggchecker.ui.theme.EstiloIconosBarraEstado
import com.adso.eggchecker.ui.theme.Yellow
import com.adso.eggchecker.ui.ventas.VentasScreen
import com.adso.eggchecker.ui.ventas.VentasViewModel

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Duración del cambio entre secciones (rápido, sin desvanecido largo). */
private const val MS_SECCION = 120

/** Tiempo mínimo visible del indicador de recarga (fluidez). */
private const val MS_REFRESCO_MINIMO = 700L

/**
 * Shell principal tras iniciar sesión: topbar + drawer + navegación
 * entre los módulos (por ahora, pantallas placeholder).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(
    viewModel: ShellViewModel,
    factory: ViewModelProvider.Factory,
    refreshBus: RefreshBus,
    ventaPendienteBus: VentaPendienteBus
) {
    val usuario by viewModel.usuario.collectAsState()
    val notificacionesViewModel: NotificacionesViewModel =
        viewModel(factory = factory)
    val estadoNotificaciones by notificacionesViewModel.estado.collectAsState()

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route
    var notificacionesAbierto by remember { mutableStateOf(false) }
    var perfilAbierto by remember { mutableStateOf(false) }
    var refrescando by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()

    // El topbar es marrón: íconos de la barra de estado en claro.
    EstiloIconosBarraEstado(oscuros = false)

    val irA: (String) -> Unit = { ruta ->
        scope.launch { drawerState.close() }
        navController.navigate(ruta) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        drawerContent = {
            AppDrawer(rutaActual = rutaActual, onSeleccionar = irA)
        }
    ) {
        Scaffold(
            containerColor = Cream,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                AppTopbar(
                    hayNoLeidas = estadoNotificaciones.hayNoLeidas,
                    onMenu = { scope.launch { drawerState.open() } },
                    onNotificaciones = {
                        notificacionesAbierto = !notificacionesAbierto
                        if (notificacionesAbierto) {
                            perfilAbierto = false
                            notificacionesViewModel.cargar()
                        }
                    },
                    onPerfilClick = {
                        perfilAbierto = !perfilAbierto
                        if (perfilAbierto) {
                            notificacionesAbierto = false
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                PullToRefreshBox(
                    isRefreshing = refrescando,
                    onRefresh = {
                        refrescando = true
                        scope.launch {
                            val inicio = System.currentTimeMillis()
                            viewModel.refrescar()
                            notificacionesViewModel.cargar()
                            refreshBus.solicitar()
                            val restante = MS_REFRESCO_MINIMO -
                                (System.currentTimeMillis() - inicio)
                            if (restante > 0) delay(restante)
                            refrescando = false
                        }
                    },
                    state = pullState,
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullState,
                            isRefreshing = refrescando,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = Yellow,
                            color = Brown
                        )
                    }
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Rutas.DASHBOARD,
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = { fadeIn(tween(MS_SECCION)) },
                        exitTransition = { fadeOut(tween(MS_SECCION)) },
                        popEnterTransition = { fadeIn(tween(MS_SECCION)) },
                        popExitTransition = { fadeOut(tween(MS_SECCION)) }
                    ) {
                    MODULOS.forEach { modulo ->
                        composable(modulo.ruta) {
                            when (modulo.ruta) {
                                Rutas.CAMADAS -> {
                                    val camadasViewModel: CamadasViewModel =
                                        viewModel(factory = factory)
                                    CamadasScreen(viewModel = camadasViewModel)
                                }
                                Rutas.PRODUCCION -> {
                                    val produccionViewModel: ProduccionViewModel =
                                        viewModel(factory = factory)
                                    ProduccionScreen(viewModel = produccionViewModel)
                                }
                                Rutas.INVENTARIO -> {
                                    val inventarioViewModel: InventarioViewModel =
                                        viewModel(factory = factory)
                                    InventarioScreen(viewModel = inventarioViewModel)
                                }
                                Rutas.CLIENTES -> {
                                    val clientesViewModel: ClientesViewModel =
                                        viewModel(factory = factory)
                                    ClientesScreen(
                                        viewModel = clientesViewModel,
                                        onRegistrarVenta = { cliente ->
                                            ventaPendienteBus.solicitar(
                                                cliente.idCliente
                                            )
                                            irA(Rutas.VENTAS)
                                        }
                                    )
                                }
                                Rutas.VENTAS -> {
                                    val ventasViewModel: VentasViewModel =
                                        viewModel(factory = factory)
                                    VentasScreen(viewModel = ventasViewModel)
                                }
                                else -> ModulePlaceholderScreen(
                                    titulo = modulo.nombre
                                )
                            }
                        }
                    }
                    }
                }

                val interaccion = remember { MutableInteractionSource() }
                val interaccionPerfil = remember { MutableInteractionSource() }

                AnimatedVisibility(
                    visible = notificacionesAbierto,
                    enter = fadeIn(
                        tween(250, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(
                        tween(200, easing = FastOutLinearInEasing)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                            .clickable(
                                interactionSource = interaccion,
                                indication = null
                            ) { notificacionesAbierto = false }
                    )
                }

                AnimatedVisibility(
                    visible = notificacionesAbierto,
                    modifier = Modifier.align(Alignment.TopEnd),
                    enter = fadeIn(tween(160)) + scaleIn(
                        initialScale = 0.7f,
                        transformOrigin = TransformOrigin(0.92f, 0f),
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut(tween(140)) + scaleOut(
                        targetScale = 0.85f,
                        transformOrigin = TransformOrigin(0.92f, 0f),
                        animationSpec = tween(
                            160,
                            easing = FastOutLinearInEasing
                        )
                    )
                ) {
                    NotificacionesPanel(
                        estado = estadoNotificaciones,
                        onMarcarTodas = notificacionesViewModel::marcarTodas,
                        onMarcarLeida = notificacionesViewModel::marcarLeida,
                        onEliminar = notificacionesViewModel::eliminar,
                        onVer = { aviso ->
                            notificacionesAbierto = false
                            val ruta = if (aviso.idInsumo != null) {
                                Rutas.INVENTARIO
                            } else {
                                Rutas.CAMADAS
                            }
                            irA(ruta)
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .widthIn(max = 400.dp)
                            .fillMaxWidth()
                    )
                }

                AnimatedVisibility(
                    visible = perfilAbierto,
                    enter = fadeIn(
                        tween(250, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(
                        tween(200, easing = FastOutLinearInEasing)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                            .clickable(
                                interactionSource = interaccionPerfil,
                                indication = null
                            ) { perfilAbierto = false }
                    )
                }

                AnimatedVisibility(
                    visible = perfilAbierto,
                    modifier = Modifier.align(Alignment.TopEnd),
                    enter = fadeIn(tween(160)) + scaleIn(
                        initialScale = 0.8f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut(tween(120)) + scaleOut(
                        targetScale = 0.9f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = tween(140)
                    )
                ) {
                    PerfilMenuCard(
                        nombre = usuario?.nombreCompleto ?: "Invitado",
                        plan = usuario?.planSuscripcion ?: "gratuito",
                        onCerrarSesion = {
                            perfilAbierto = false
                            viewModel.cerrarSesion()
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .width(240.dp)
                    )
                }
            }
        }
    }
}
