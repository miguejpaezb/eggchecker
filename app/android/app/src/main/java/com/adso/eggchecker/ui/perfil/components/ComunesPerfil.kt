package com.adso.eggchecker.ui.perfil.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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

import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.SuccessText
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.ErrorRed

/** Tarjeta contenedora de las secciones del perfil. */
@Composable
fun TarjetaPerfil(
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), content = contenido)
    }
}

/** Subtítulo de sección dentro de un formulario. */
@Composable
fun SubtituloSeccion(texto: String) {
    Text(
        text = texto,
        fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = Brown,
        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
    )
}

/** Mensaje de error del formulario. */
@Composable
fun AvisoError(texto: String) {
    Text(
        text = texto,
        color = ErrorRed,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

/** Mensaje de éxito del formulario. */
@Composable
fun AvisoExito(texto: String) {
    Text(
        text = texto,
        color = SuccessText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

/** Botones de acción del formulario (cancelar + guardar). */
@Composable
fun AccionesFormulario(
    textoGuardar: String,
    enviando: Boolean,
    onCancelar: () -> Unit,
    onGuardar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.End
    ) {
        BotonApp(
            texto = "Cancelar",
            onClick = onCancelar,
            tipo = TipoBoton.GHOST,
            habilitado = !enviando
        )
        BotonApp(
            texto = if (enviando) "Guardando…" else textoGuardar,
            onClick = onGuardar,
            tipo = TipoBoton.PRIMARIO,
            cargando = enviando,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
