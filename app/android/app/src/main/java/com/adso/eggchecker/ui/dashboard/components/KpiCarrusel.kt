package com.adso.eggchecker.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

import com.adso.eggchecker.ui.theme.Brown

import kotlinx.coroutines.launch

/** Carrusel horizontal de tarjetas KPI con puntos de posición. */
@Composable
fun KpiCarrusel(kpis: List<Kpi>, modifier: Modifier = Modifier) {
    if (kpis.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { kpis.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { pagina ->
            KpiCard(kpi = kpis[pagina])
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            kpis.indices.forEach { indice ->
                val activo = pagerState.currentPage == indice
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (activo) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (activo) Brown else Brown.copy(alpha = 0.3f)
                        )
                        .clickable {
                            scope.launch {
                                pagerState.animateScrollToPage(indice)
                            }
                        }
                )
            }
        }
    }
}
