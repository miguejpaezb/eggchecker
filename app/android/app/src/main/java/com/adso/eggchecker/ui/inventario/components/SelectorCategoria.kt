package com.adso.eggchecker.ui.inventario.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Categoria
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.InputBackground
import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.TextMuted

/** Selector de categoría existente (solo permite elegir del catálogo). */
@Composable
fun SelectorCategoria(
    categorias: List<Categoria>,
    seleccionada: Categoria?,
    onSeleccionar: (Categoria) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String = "Categoría"
) {
    var abierto by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = etiqueta,
            fontSize = 14.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = InputBackground,
            border = BorderStroke(1.dp, Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { abierto = true }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = seleccionada?.nombreCateg
                            ?: "Selecciona una categoría",
                        fontSize = 15.sp,
                        color = if (seleccionada != null) Dark else Placeholder
                    )
                    Text(text = "▾", color = Placeholder)
                }
                DropdownMenu(
                    expanded = abierto,
                    onDismissRequest = { abierto = false }
                ) {
                    categorias.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria.nombreCateg) },
                            onClick = {
                                abierto = false
                                onSeleccionar(categoria)
                            }
                        )
                    }
                }
            }
        }
    }
}
