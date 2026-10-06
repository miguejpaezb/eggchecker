package com.adso.eggchecker.ui.inventario.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

import com.adso.eggchecker.domain.model.Insumo
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.inventario.formatearCantidad
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Cream
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.TextMuted

/** Modal para añadir stock (movimiento de entrada). */
@Composable
fun AnadirStockModal(
    abierto: Boolean,
    insumo: Insumo?,
    onCerrar: () -> Unit,
    onAnadir: (
        idInsumo: Int,
        cantidad: Double,
        onResultado: (Boolean) -> Unit
    ) -> Unit
) {
    var cantidad by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(abierto) {
        if (abierto) {
            cantidad = ""
            enviando = false
            error = null
        }
    }

    ModalApp(abierto = abierto, titulo = "Añadir Stock", onCerrar = onCerrar) {
        if (insumo != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Cream,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = insumo.nombreInsumo,
                        fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Brown
                    )
                    Text(
                        text = "Stock actual: ${formatearCantidad(insumo.stockActual)} " +
                            insumo.unidadMedida,
                        fontSize = 13.6.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Stock mínimo: ${formatearCantidad(insumo.umbralMinimo)} " +
                            insumo.unidadMedida,
                        fontSize = 13.6.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        error?.let {
            Text(
                text = it,
                color = ErrorRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        CampoTexto(
            valor = cantidad,
            onValorChange = { cantidad = it },
            etiqueta = "Cantidad a añadir",
            marcador = "Ej. 20",
            keyboardType = KeyboardType.Decimal,
            soloDecimal = true,
            hint = "La cantidad se suma al stock actual, sin importar el mínimo."
        )

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
                texto = "Añadir stock",
                onClick = {
                    error = null
                    val objetivo = insumo ?: return@BotonApp
                    val numero = cantidad.toDoubleOrNull()
                    if (numero == null || numero <= 0.0) {
                        error = "La cantidad debe ser mayor que cero"
                    } else {
                        enviando = true
                        onAnadir(objetivo.idInsumo, numero) { exito ->
                            enviando = false
                            if (exito) onCerrar()
                        }
                    }
                },
                tipo = TipoBoton.PRIMARIO,
                cargando = enviando,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
