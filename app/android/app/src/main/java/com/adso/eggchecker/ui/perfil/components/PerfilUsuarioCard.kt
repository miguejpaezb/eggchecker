package com.adso.eggchecker.ui.perfil.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.R
import com.adso.eggchecker.ui.perfil.textoPlan
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.theme.Yellow

/** Tarjeta con el avatar y los datos básicos del usuario. */
@Composable
fun PerfilUsuarioCard(
    nombre: String,
    correo: String,
    plan: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Yellow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_usuario),
                    contentDescription = null,
                    tint = Brown,
                    modifier = Modifier.size(44.dp)
                )
            }
            Text(
                text = nombre,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Dark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = correo,
                fontSize = 14.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = Yellow.copy(alpha = 0.25f),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    text = textoPlan(plan),
                    color = Brown,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}
