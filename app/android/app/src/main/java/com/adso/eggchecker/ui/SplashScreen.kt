package com.adso.eggchecker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import com.adso.eggchecker.R
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Cream

/** Pantalla breve mientras se resuelve la sesión guardada. */
@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo_eggchecker),
            contentDescription = "Isotipo EggChecker",
            modifier = Modifier.size(140.dp)
        )
        CircularProgressIndicator(color = Brown)
    }
}
