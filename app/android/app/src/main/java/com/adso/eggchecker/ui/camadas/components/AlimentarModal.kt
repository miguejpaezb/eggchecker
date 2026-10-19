package com.adso.eggchecker.ui.camadas.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.domain.model.Insumo
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.SelectorPill
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.inventario.formatearCantidad
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Modal para registrar el alimento consumido por una camada.
 *
 * Solo recibe los insumos de categoría alimento con stock disponible y
 * limita la cantidad al stock actual antes de pedir confirmación.
 */
@Composable
fun AlimentarModal(
    abierto: Boolean,
    camada: Camada?,
    alimentos: List<Insumo>,
    onCerrar: () -> Unit,
    onAlimentar: (
        idCamada: Int,
        idInsumo: Int,
        cantidad: Double,
        onResultado: (Boolean) -> Unit
    ) -> Unit
) {
    var idInsumo by remember { mutableStateOf<Int?>(null) }
    var cantidad by remember { mutableStateOf("") }
    var confirmando by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(abierto) {
        if (abierto) {
            idInsumo = null
            cantidad = ""
            confirmando = false
            enviando = false
            error = null
        }
    }

    val insumo = alimentos.firstOrNull { it.idInsumo == idInsumo }
    val stock = insumo?.stockActual ?: 0.0
    val unidad = insumo?.unidadMedida ?: ""

    ModalApp(
        abierto = abierto,
        titulo = "Alimentar camada",
        onCerrar = onCerrar
    ) {
        if (confirmando && camada != null && insumo != null) {
            Text(
                text = "¿Alimentar ${camada.nombreCamada} con " +
                    "${cantidad.ifBlank { "0" }} $unidad de " +
                    "${insumo.nombreInsumo}? Se descontará del inventario.",
                fontSize = 14.sp,
                color = Dark
            )
            error?.let {
                Text(
                    text = it,
                    color = ErrorRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                BotonApp(
                    texto = "Volver",
                    onClick = { confirmando = false },
                    tipo = TipoBoton.GHOST,
                    habilitado = !enviando
                )
                BotonApp(
                    texto = "Confirmar",
                    onClick = {
                        val numero = cantidad.replace(',', '.').toDoubleOrNull()
                        if (numero == null || numero <= 0.0) {
                            error = "La cantidad debe ser mayor que cero"
                            return@BotonApp
                        }
                        enviando = true
                        error = null
                        onAlimentar(
                            camada.idCamada,
                            insumo.idInsumo,
                            numero
                        ) { exito ->
                            enviando = false
                            if (exito) onCerrar()
                        }
                    },
                    cargando = enviando,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            return@ModalApp
        }

        Text(
            text = "Alimento",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Dark,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        if (alimentos.isEmpty()) {
            Text(
                text = "No tienes insumos de categoría alimento con stock " +
                    "disponible. Añade stock en Inventario para poder alimentar.",
                fontSize = 13.sp,
                color = TextMuted
            )
        } else {
            SelectorPill(
                etiqueta = "ALIMENTO",
                valorTexto = insumo?.nombreInsumo ?: "Elegir",
                opciones = alimentos.map { alimento ->
                    alimento.idInsumo.toString() to
                        "${alimento.nombreInsumo} · ${alimento.unidadMedida}"
                },
                onSeleccionar = { valor ->
                    idInsumo = valor.toIntOrNull()
                    cantidad = ""
                    error = null
                }
            )
        }

        if (insumo != null) {
            Text(
                text = "Stock disponible: ${formatearCantidad(stock)} $unidad",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Brown,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        CampoTexto(
            valor = cantidad,
            onValorChange = { texto ->
                // No se permite escribir por encima del stock disponible.
                val numero = texto.replace(',', '.').toDoubleOrNull()
                if (insumo != null && numero != null && numero > stock) {
                    cantidad = formatearCantidad(stock)
                    error = "La cantidad no puede superar el stock disponible"
                } else {
                    cantidad = texto
                    error = null
                }
            },
            etiqueta = if (insumo != null) {
                "Cantidad ($unidad)"
            } else {
                "Cantidad"
            },
            marcador = "Ej. 12.5",
            keyboardType = KeyboardType.Decimal,
            soloDecimal = true,
            habilitado = insumo != null,
            hint = "No puede superar el stock actual del alimento.",
            modifier = Modifier.padding(top = 12.dp)
        )

        error?.let {
            Text(
                text = it,
                color = ErrorRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
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
                texto = "Continuar",
                onClick = {
                    error = null
                    if (insumo == null) {
                        error = "Elige el alimento"
                    } else {
                        val numero = cantidad.replace(',', '.').toDoubleOrNull()
                        when {
                            numero == null || numero <= 0.0 -> error =
                                "La cantidad debe ser mayor que cero"
                            numero > stock -> error =
                                "La cantidad supera el stock disponible"
                            else -> confirmando = true
                        }
                    }
                },
                habilitado = insumo != null,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
