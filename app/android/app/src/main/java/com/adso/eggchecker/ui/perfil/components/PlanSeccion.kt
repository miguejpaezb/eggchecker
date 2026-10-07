package com.adso.eggchecker.ui.perfil.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Perfil
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.perfil.descripcionPlan
import com.adso.eggchecker.ui.perfil.textoPlan
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.SuccessBackground
import com.adso.eggchecker.ui.theme.SuccessText
import com.adso.eggchecker.ui.theme.TextMuted

/** Sección informativa del plan y la suscripción actual. */
@Composable
fun PlanSeccion(perfil: Perfil) {
    TarjetaPerfil {
        SubtituloSeccion("Mi Suscripción")

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(text = "PLAN ACTUAL", fontSize = 11.sp, color = TextMuted)
                        Text(
                            text = textoPlan(perfil.planSuscripcion),
                            fontFamily = MaterialTheme.typography.titleLarge
                                .fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Brown
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SuccessBackground
                    ) {
                        Text(
                            text = "Activo",
                            color = SuccessText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 4.dp
                            )
                        )
                    }
                }
                Text(
                    text = descripcionPlan(perfil.planSuscripcion),
                    fontSize = 14.sp,
                    color = Dark,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Próxima facturación",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "No aplica",
                            fontWeight = FontWeight.Bold,
                            color = Dark
                        )
                    }
                    BotonApp(
                        texto = "Mejorar Plan",
                        onClick = {},
                        tipo = TipoBoton.AMARILLO
                    )
                }
            }
        }
    }
}
