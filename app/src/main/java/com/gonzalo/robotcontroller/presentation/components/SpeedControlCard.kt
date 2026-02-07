package com.gonzalo.robotcontroller.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SpeedControlCard(
    speed: Int,
    onSpeedChange: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Speed Control",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "$speed%",
                style = MaterialTheme.typography.headlineMedium
            )

            Slider(
                value = speed.toFloat(),
                onValueChange = { onSpeedChange(it.toInt()) },
                valueRange = 0f..100f,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
