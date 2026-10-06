package com.adso.eggchecker.ui.inventario.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Categoria
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.TextMuted

/** Modal para registrar un insumo nuevo. */
@Composable
fun NuevoInsumoModal(
    abierto: Boolean,
    categorias: List<Categoria>,
    onCerrar: () -> Unit,
    onCrear: (
        idCategoria: Int,
        nombre: String,
        unidad: String,
        stock: Double,
        umbral: Double,
        onResultado: (Boolean) -> Unit
    ) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf<Categoria?>(null) }
    var unidad by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var umbral by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(abierto) {
        if (abierto) {
            nombre = ""
            categoria = null
            unidad = ""
            stock = ""
            umbral = ""
            enviando = false
            error = null
        }
    }

    ModalApp(abierto = abierto, titulo = "Añadir Insumo", onCerrar = onCerrar) {
        error?.let {
            Text(
                text = it,
                color = ErrorRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        CampoTexto(
            valor = nombre,
            onValorChange = { nombre = it },
            etiqueta = "Nombre del insumo",
            marcador = "Ej. Concentrado Ponedora 16%"
        )

        Column(modifier = Modifier.padding(top = 16.dp)) {
            SelectorCategoria(
                categorias = categorias,
                seleccionada = categoria,
                onSeleccionar = { categoria = it }
            )
            Text(
                text = "Solo categorías existentes; créalas en el botón Categoría.",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            CampoTexto(
                valor = unidad,
                onValorChange = { unidad = it },
                etiqueta = "Unidad de medida",
                marcador = "kg, litro, unidad…"
            )
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            CampoTexto(
                valor = stock,
                onValorChange = { stock = it },
                etiqueta = "Stock actual",
                marcador = "Ej. 50",
                keyboardType = KeyboardType.Decimal,
                soloDecimal = true
            )
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            CampoTexto(
                valor = umbral,
                onValorChange = { umbral = it },
                etiqueta = "Stock mínimo",
                marcador = "Ej. 10",
                keyboardType = KeyboardType.Decimal,
                soloDecimal = true
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
                texto = "Registrar insumo",
                onClick = {
                    error = null
                    val categoriaElegida = categoria
                    val stockNumero = stock.toDoubleOrNull()
                    val umbralNumero = umbral.toDoubleOrNull()
                    when {
                        categoriaElegida == null ->
                            error = "Selecciona una categoría existente"
                        nombre.isBlank() ->
                            error = "Ingresa el nombre del insumo"
                        unidad.isBlank() ->
                            error = "Ingresa la unidad de medida"
                        stockNumero == null || stockNumero <= 0.0 ->
                            error = "El stock actual debe ser mayor que cero"
                        umbralNumero == null || umbralNumero <= 0.0 ->
                            error = "El stock mínimo debe ser mayor que cero"
                        else -> {
                            enviando = true
                            onCrear(
                                categoriaElegida.idCategoria,
                                nombre.trim(),
                                unidad.trim(),
                                stockNumero,
                                umbralNumero
                            ) { exito ->
                                enviando = false
                                if (exito) onCerrar()
                            }
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
