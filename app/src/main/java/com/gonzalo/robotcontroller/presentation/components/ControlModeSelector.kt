package com.gonzalo.robotcontroller.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class ControlMode {
    DPad,
    Joystick
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlModeSelector(
    selectedMode: ControlMode,
    onModeSelected: (ControlMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        ControlMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = ControlMode.entries.size
                )
            ) {
                Text(
                    when (mode) {
                        ControlMode.DPad -> "D-Pad"
                        ControlMode.Joystick -> "Joystick"
                    }
                )
            }
        }
    }
}
