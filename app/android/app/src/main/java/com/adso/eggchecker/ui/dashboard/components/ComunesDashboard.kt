package com.adso.eggchecker.ui.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.SuperficieTarjeta

/** Tarjeta base del dashboard con título opcional. */
@Composable
fun TarjetaDashboard(
    titulo: String = "",
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            if (titulo.isNotEmpty()) {
                Text(
                    text = titulo,
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Brown,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            contenido()
        }
    }
}
