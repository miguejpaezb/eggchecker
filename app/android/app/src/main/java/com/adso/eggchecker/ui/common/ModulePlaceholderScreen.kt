package com.adso.eggchecker.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Pantalla base de cada módulo mientras se implementa.
 *
 * @param titulo Nombre del módulo (ej. "Inventario").
 */
@Composable
fun ModulePlaceholderScreen(
    titulo: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 30.sp,
            color = Brown,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Módulo en construcción.",
            fontSize = 16.sp,
            color = TextMuted
        )
        Text(
            text = "Este módulo todavía no está implementado. La estructura " +
                "de navegación ya está lista para conectarlo.",
            color = TextMuted,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
