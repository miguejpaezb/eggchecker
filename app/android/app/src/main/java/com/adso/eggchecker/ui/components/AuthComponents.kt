package com.adso.eggchecker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.R
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorBackground
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.ErrorText
import com.adso.eggchecker.ui.theme.Green
import com.adso.eggchecker.ui.theme.InputBackground
import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.SuccessBackground
import com.adso.eggchecker.ui.theme.SuccessText
import com.adso.eggchecker.ui.theme.Yellow
import com.adso.eggchecker.ui.theme.YellowHover

/**
 * Estructura visual de las pantallas de autenticación: fondo crema con
 * patrón, marca EggChecker y tarjeta blanca centrada.
 *
 * @param contenido Campos y acciones que van dentro de la tarjeta.
 * @param pie Enlace para alternar entre iniciar sesión y registrarse.
 */
@Composable
fun AuthScaffold(
    contenido: @Composable ColumnScope.() -> Unit,
    pie: @Composable () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.patron_fondo_eggchecker),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .alpha(0.2f),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_eggchecker),
                contentDescription = "Isotipo EggChecker",
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = "EggChecker",
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Brown
            )
            Text(
                text = "Cada huevo cuenta",
                fontSize = 16.sp,
                color = Green,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 448.dp)
                    .shadow(25.dp, RoundedCornerShape(25.dp)),
                shape = RoundedCornerShape(25.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 40.dp,
                        vertical = 20.dp
                    ),
                    content = contenido
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            pie()
        }
    }
}

/**
 * Campo de texto con etiqueta, ícono y borde redondeado, fiel al web.
 *
 * @param valor Texto actual.
 * @param onValorChange Callback al cambiar el texto.
 * @param etiqueta Etiqueta visible del campo.
 * @param icono Ícono del campo.
 * @param error Mensaje de error a mostrar.
 */
@Composable
fun AuthTextField(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    marcador: String = "",
    error: String? = null,
    esContrasena: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    habilitado: Boolean = true
) {
    var contrasenaVisible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val enfocado by interactionSource.collectIsFocusedAsState()

    val colorBorde = when {
        error != null -> ErrorRed
        enfocado -> Brown
        else -> Border
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = etiqueta,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Dark,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = InputBackground,
            border = BorderStroke(1.dp, colorBorde),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = Placeholder,
                    modifier = Modifier.size(20.dp)
                )
                Box(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    if (valor.isEmpty()) {
                        Text(
                            text = marcador.ifBlank { etiqueta },
                            fontSize = 14.sp,
                            color = Placeholder
                        )
                    }
                    BasicTextField(
                        value = valor,
                        onValueChange = onValorChange,
                        enabled = habilitado,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Dark
                        ),
                        cursorBrush = SolidColor(Brown),
                        interactionSource = interactionSource,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = imeAction
                        ),
                        visualTransformation = if (esContrasena && !contrasenaVisible) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = if (esContrasena) 28.dp else 0.dp)
                    )
                    if (esContrasena) {
                        Icon(
                            imageVector = if (contrasenaVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (contrasenaVisible) {
                                "Ocultar contraseña"
                            } else {
                                "Mostrar contraseña"
                            },
                            tint = Placeholder,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(20.dp)
                                .clickable {
                                    contrasenaVisible = !contrasenaVisible
                                }
                        )
                    }
                }
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

/**
 * Botón principal amarillo de las pantallas de autenticación.
 *
 * @param texto Texto del botón.
 * @param onClick Acción al pulsar.
 * @param cargando Deshabilita el botón y muestra un indicador.
 */
@Composable
fun AuthButton(
    texto: String,
    onClick: () -> Unit,
    cargando: Boolean = false,
    habilitado: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Yellow,
            contentColor = Brown,
            disabledContainerColor = YellowHover,
            disabledContentColor = Brown
        )
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Brown,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text(
            text = texto,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

/** Mensaje de error o éxito dentro de la tarjeta de autenticación. */
@Composable
fun AuthAlert(mensaje: String, esError: Boolean) {
    val fondo = if (esError) ErrorBackground else SuccessBackground
    val texto = if (esError) ErrorText else SuccessText
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = fondo,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = mensaje,
            color = texto,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
