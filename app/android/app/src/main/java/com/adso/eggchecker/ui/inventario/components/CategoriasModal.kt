package com.adso.eggchecker.ui.inventario.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Categoria
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted

/** Modal para crear, editar y eliminar categorías de insumo. */
@Composable
fun CategoriasModal(
    abierto: Boolean,
    categorias: List<Categoria>,
    onCerrar: () -> Unit,
    onCrear: (
        nombre: String,
        descripcion: String?,
        onResultado: (Boolean) -> Unit
    ) -> Unit,
    onEditar: (
        idCategoria: Int,
        nombre: String,
        descripcion: String?,
        onResultado: (Boolean) -> Unit
    ) -> Unit,
    onEliminar: (idCategoria: Int, onResultado: (Boolean) -> Unit) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var editando by remember { mutableStateOf<Categoria?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var aEliminar by remember { mutableStateOf<Categoria?>(null) }
    var eliminando by remember { mutableStateOf(false) }
    var errorEliminar by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(abierto) {
        if (abierto) {
            nombre = ""
            descripcion = ""
            editando = null
            error = null
            guardando = false
            aEliminar = null
            eliminando = false
            errorEliminar = null
        }
    }

    ModalApp(abierto = abierto, titulo = "Categorías", onCerrar = onCerrar) {
        val enEliminar = aEliminar
        if (enEliminar != null) {
            Text(
                text = "¿Eliminar la categoría \"${enEliminar.nombreCateg}\"? " +
                    "Solo se puede si no tiene insumos asociados.",
                color = Dark,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            errorEliminar?.let {
                Text(
                    text = it,
                    color = ErrorRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                BotonApp(
                    texto = "Volver",
                    onClick = { aEliminar = null },
                    tipo = TipoBoton.GHOST
                )
                BotonApp(
                    texto = "Eliminar",
                    onClick = {
                        errorEliminar = null
                        eliminando = true
                        onEliminar(enEliminar.idCategoria) { exito ->
                            eliminando = false
                            if (exito) aEliminar = null
                        }
                    },
                    tipo = TipoBoton.PELIGRO,
                    cargando = eliminando,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            return@ModalApp
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
            valor = nombre,
            onValorChange = { nombre = it },
            etiqueta = if (editando != null) {
                "Editar categoría"
            } else {
                "Nueva categoría"
            },
            marcador = "Ej. Vitaminas"
        )

        Column(modifier = Modifier.padding(top = 16.dp)) {
            CampoTexto(
                valor = descripcion,
                onValorChange = { descripcion = it },
                etiqueta = "Descripción (opcional)"
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            val enEdicion = editando
            if (enEdicion != null) {
                BotonApp(
                    texto = "Cancelar",
                    onClick = {
                        nombre = ""
                        descripcion = ""
                        editando = null
                        error = null
                    },
                    tipo = TipoBoton.GHOST
                )
            }
            BotonApp(
                texto = if (enEdicion != null) {
                    "Guardar"
                } else {
                    "Agregar categoría"
                },
                onClick = {
                    error = null
                    val limpio = nombre.trim()
                    if (limpio.isEmpty()) {
                        error = "Ingresa el nombre de la categoría"
                        return@BotonApp
                    }
                    val desc = descripcion.trim().ifBlank { null }
                    guardando = true
                    val alGuardar: (Boolean) -> Unit = { exito ->
                        guardando = false
                        if (exito) {
                            nombre = ""
                            descripcion = ""
                            editando = null
                        }
                    }
                    if (enEdicion != null) {
                        onEditar(enEdicion.idCategoria, limpio, desc, alGuardar)
                    } else {
                        onCrear(limpio, desc, alGuardar)
                    }
                },
                tipo = TipoBoton.PRIMARIO,
                cargando = guardando,
                modifier = if (enEdicion != null) {
                    Modifier.padding(start = 12.dp)
                } else {
                    Modifier
                }
            )
        }

        if (categorias.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categorias.forEach { categoria ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SuperficieTarjeta,
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 10.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = categoria.nombreCateg,
                                    fontWeight = FontWeight.Bold,
                                    color = Dark
                                )
                                categoria.descripcion?.let { texto ->
                                    Text(
                                        text = texto,
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            BotonApp(
                                texto = "Editar",
                                onClick = {
                                    editando = categoria
                                    nombre = categoria.nombreCateg
                                    descripcion = categoria.descripcion.orEmpty()
                                    error = null
                                },
                                tipo = TipoBoton.GHOST
                            )
                            BotonApp(
                                texto = "Eliminar",
                                onClick = {
                                    errorEliminar = null
                                    aEliminar = categoria
                                },
                                tipo = TipoBoton.GHOST,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
