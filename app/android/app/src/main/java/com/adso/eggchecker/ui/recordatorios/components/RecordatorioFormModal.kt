package com.adso.eggchecker.ui.recordatorios.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.domain.model.Categoria
import com.adso.eggchecker.domain.model.Insumo
import com.adso.eggchecker.domain.model.MODO_CADA_DIAS
import com.adso.eggchecker.domain.model.MODO_DIAS_SEMANA
import com.adso.eggchecker.domain.model.MODO_FECHAS
import com.adso.eggchecker.domain.model.Recordatorio
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.SelectorPill
import com.adso.eggchecker.ui.components.SelectorSegmentado
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.perfil.components.SwitchApp
import com.adso.eggchecker.ui.recordatorios.CATEGORIAS_RECORDATORIO
import com.adso.eggchecker.ui.recordatorios.DIAS_SEMANA_RECORDATORIO
import com.adso.eggchecker.ui.recordatorios.DatosRecordatorio
import com.adso.eggchecker.ui.recordatorios.MODOS_RECORDATORIO
import com.adso.eggchecker.ui.recordatorios.RecordatoriosViewModel
import com.adso.eggchecker.ui.recordatorios.formatoCantidad
import com.adso.eggchecker.ui.recordatorios.hoyIsoRecordatorio
import com.adso.eggchecker.ui.recordatorios.textoCantidad
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.theme.Yellow

/** Máximo de fechas exactas que acepta el backend. */
private const val MAXIMO_FECHAS = 4

/**
 * Formulario para crear o editar un recordatorio sanitario.
 *
 * @param camada Camada desde cuyo menú se abre.
 * @param recordatorio Regla a editar; nulo para crear una nueva.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordatorioFormModal(
    abierto: Boolean,
    camada: Camada?,
    recordatorio: Recordatorio?,
    viewModel: RecordatoriosViewModel,
    onCerrar: () -> Unit,
    onGuardar: (DatosRecordatorio) -> Unit
) {
    var alcance by remember { mutableStateOf("camada") }
    var categoria by remember { mutableStateOf("medicamento") }
    var insumos by remember { mutableStateOf<List<Insumo>>(emptyList()) }
    var categorias by remember { mutableStateOf<List<Categoria>>(emptyList()) }
    var idInsumo by remember { mutableStateOf<Int?>(null) }
    var cantidad by remember { mutableStateOf("5") }
    var modo by remember { mutableStateOf(MODO_CADA_DIAS) }
    var intervalo by remember { mutableStateOf("15") }
    var dias by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var fechas by remember { mutableStateOf<List<String>>(emptyList()) }
    var fechaInicio by remember { mutableStateOf(hoyIsoRecordatorio()) }
    var fechaFin by remember { mutableStateOf<String?>(null) }
    var activo by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(abierto, recordatorio?.idRecordatorio) {
        if (!abierto) return@LaunchedEffect
        viewModel.cargarCatalogos { listaInsumos, listaCategorias ->
            insumos = listaInsumos
            categorias = listaCategorias
            val regla = recordatorio
            if (regla == null) {
                alcance = "camada"
                categoria = "medicamento"
                idInsumo = null
                cantidad = "5"
                modo = MODO_CADA_DIAS
                intervalo = "15"
                dias = emptySet()
                fechas = emptyList()
                fechaInicio = hoyIsoRecordatorio()
                fechaFin = null
                activo = true
            } else {
                alcance = if (regla.esGlobal) "global" else "camada"
                val insumoRegla = listaInsumos.firstOrNull {
                    it.idInsumo == regla.idInsumo
                }
                val categoriaRegla = listaCategorias.firstOrNull {
                    it.idCategoria == insumoRegla?.idCategoria
                }
                categoria = if (categoriaRegla?.nombreCateg == "vitamina") {
                    "vitamina"
                } else {
                    "medicamento"
                }
                idInsumo = regla.idInsumo
                cantidad = formatoCantidad(regla.cantidad)
                modo = regla.modo
                intervalo = (regla.intervaloDias ?: 15).toString()
                dias = regla.diasSemana.toSet()
                fechas = regla.fechas
                fechaInicio = regla.fechaInicio
                fechaFin = regla.fechaFin
                activo = regla.activo
            }
            error = null
        }
    }

    val idCategoriaElegida = categorias.firstOrNull {
        it.nombreCateg == categoria
    }?.idCategoria
    val opcionesInsumo = insumos.filter {
        it.idCategoria == idCategoriaElegida && it.activo && !it.descontinuado
    }
    val insumoElegido = insumos.firstOrNull { it.idInsumo == idInsumo }

    ModalApp(
        abierto = abierto,
        titulo = if (recordatorio == null) {
            "Nuevo recordatorio"
        } else {
            "Editar recordatorio"
        },
        onCerrar = onCerrar
    ) {
        Text(
            text = "Alcance",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        SelectorSegmentado(
            opciones = listOf(
                "camada" to (camada?.nombreCamada ?: "Esta camada"),
                "global" to "Todas"
            ),
            valor = alcance,
            onValorChange = { alcance = it }
        )

        Text(
            text = "Tipo de insumo",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
        )
        SelectorSegmentado(
            opciones = CATEGORIAS_RECORDATORIO,
            valor = categoria,
            onValorChange = {
                categoria = it
                idInsumo = null
            }
        )

        SelectorPill(
            etiqueta = "INSUMO",
            valorTexto = insumoElegido?.nombreInsumo ?: "Elegir",
            opciones = opcionesInsumo.map { insumo ->
                insumo.idInsumo.toString() to
                    "${insumo.nombreInsumo} · ${insumo.unidadMedida}"
            },
            onSeleccionar = { valor ->
                idInsumo = valor.toIntOrNull()
                error = null
            },
            modifier = Modifier.padding(top = 12.dp)
        )
        if (opcionesInsumo.isEmpty()) {
            Text(
                text = "No tienes insumos activos de esta categoría.",
                fontSize = 12.sp,
                color = ErrorRed,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else if (insumoElegido != null) {
            Text(
                text = "Stock actual: " + textoCantidad(
                    insumoElegido.stockActual,
                    insumoElegido.unidadMedida
                ),
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        CampoTexto(
            valor = cantidad,
            onValorChange = {
                cantidad = it
                error = null
            },
            etiqueta = "Dosis por camada" +
                (insumoElegido?.let { " (${it.unidadMedida})" } ?: ""),
            keyboardType = KeyboardType.Decimal,
            soloDecimal = true,
            hint = "Se descuenta del inventario al registrar la aplicación.",
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Frecuencia",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
        )
        SelectorSegmentado(
            opciones = MODOS_RECORDATORIO,
            valor = modo,
            onValorChange = {
                modo = it
                error = null
            }
        )

        when (modo) {
            MODO_CADA_DIAS -> {
                CampoTexto(
                    valor = intervalo,
                    onValorChange = {
                        intervalo = it.filter { c -> c.isDigit() }
                        error = null
                    },
                    etiqueta = "Cada cuántos días",
                    keyboardType = KeyboardType.Number,
                    hint = "Por ejemplo 7, 15 o 30 días.",
                    modifier = Modifier.padding(top = 12.dp)
                )
                SelectorFechaRecordatorio(
                    etiqueta = "Fecha de inicio",
                    fecha = fechaInicio,
                    onCambiar = { fechaInicio = it }
                )
            }
            MODO_DIAS_SEMANA -> {
                Text(
                    text = "Días de la semana",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DIAS_SEMANA_RECORDATORIO.forEachIndexed { indice, nombre ->
                        ChipDia(
                            texto = nombre,
                            activo = indice in dias,
                            onClick = {
                                dias = if (indice in dias) {
                                    dias - indice
                                } else {
                                    dias + indice
                                }
                                error = null
                            }
                        )
                    }
                }
                SelectorFechaRecordatorio(
                    etiqueta = "Fecha de inicio",
                    fecha = fechaInicio,
                    modifier = Modifier.padding(top = 12.dp),
                    onCambiar = { fechaInicio = it }
                )
            }
            MODO_FECHAS -> {
                Text(
                    text = "Fechas de aplicación",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                )
                fechas.forEachIndexed { indice, fecha ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SelectorFechaRecordatorio(
                                etiqueta = "Fecha ${indice + 1}",
                                fecha = fecha,
                                onCambiar = { nueva ->
                                    fechas = fechas.toMutableList().also {
                                        it[indice] = nueva
                                    }
                                }
                            )
                        }
                        Text(
                            text = "×",
                            fontSize = 20.sp,
                            color = ErrorRed,
                            modifier = Modifier
                                .clickable {
                                    fechas = fechas.filterIndexed { i, _ ->
                                        i != indice
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                if (fechas.size < MAXIMO_FECHAS) {
                    BotonApp(
                        texto = "Agregar fecha",
                        onClick = { fechas = fechas + hoyIsoRecordatorio() },
                        tipo = TipoBoton.GHOST
                    )
                }
            }
        }

        if (modo != MODO_FECHAS) {
            SelectorFechaRecordatorio(
                etiqueta = "Fecha de fin (opcional)",
                fecha = fechaFin,
                minima = fechaInicio,
                modifier = Modifier.padding(top = 12.dp),
                onCambiar = { fechaFin = it }
            )
            if (fechaFin != null) {
                Text(
                    text = "Quitar fecha de fin",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brown,
                    modifier = Modifier
                        .clickable { fechaFin = null }
                        .padding(top = 8.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recordatorio activo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Dark,
                modifier = Modifier.weight(1f)
            )
            SwitchApp(activo = activo, onCambiar = { activo = it })
        }

        if (error != null) {
            Text(
                text = error.orEmpty(),
                fontSize = 12.sp,
                color = ErrorRed,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            BotonApp(
                texto = "Cancelar",
                onClick = onCerrar,
                tipo = TipoBoton.GHOST
            )
            BotonApp(
                texto = "Guardar",
                onClick = {
                    val valorCantidad = cantidad.replace(',', '.').toDoubleOrNull()
                    val intervaloNumero = intervalo.toIntOrNull()
                    error = when {
                        insumoElegido == null ->
                            "Elige el medicamento o la vitamina."
                        valorCantidad == null || valorCantidad <= 0 ->
                            "La dosis debe ser mayor que cero."
                        modo == MODO_CADA_DIAS &&
                            (intervaloNumero == null ||
                                intervaloNumero !in 1..365) ->
                            "El intervalo debe estar entre 1 y 365 días."
                        modo == MODO_DIAS_SEMANA && dias.isEmpty() ->
                            "Elige al menos un día de la semana."
                        modo == MODO_FECHAS && fechas.isEmpty() ->
                            "Agrega al menos una fecha."
                        else -> null
                    }
                    if (error == null && insumoElegido != null &&
                        valorCantidad != null
                    ) {
                        onGuardar(
                            DatosRecordatorio(
                                idCamada = if (alcance == "global") {
                                    null
                                } else {
                                    camada?.idCamada
                                },
                                idInsumo = insumoElegido.idInsumo,
                                modo = modo,
                                intervaloDias = if (modo == MODO_CADA_DIAS) {
                                    intervaloNumero
                                } else {
                                    null
                                },
                                diasSemana = if (modo == MODO_DIAS_SEMANA) {
                                    dias.sorted()
                                } else {
                                    null
                                },
                                fechas = if (modo == MODO_FECHAS) {
                                    fechas.sorted()
                                } else {
                                    null
                                },
                                fechaInicio = if (modo == MODO_FECHAS) {
                                    null
                                } else {
                                    fechaInicio
                                },
                                fechaFin = if (modo == MODO_FECHAS) {
                                    null
                                } else {
                                    fechaFin
                                },
                                cantidad = valorCantidad,
                                activo = activo
                            )
                        )
                    }
                },
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

/** Chip seleccionable para los días de la semana. */
@Composable
private fun ChipDia(
    texto: String,
    activo: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (activo) Brown else Color.Transparent,
        border = BorderStroke(1.dp, if (activo) Brown else Border),
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Text(
            text = texto,
            color = if (activo) Yellow else TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
