package com.gonzalo.robotcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.gonzalo.robotcontroller.data.preferences.SettingsDataStore
import com.gonzalo.robotcontroller.data.repository.RobotRepository
import com.gonzalo.robotcontroller.data.websocket.WebSocketClient
import com.gonzalo.robotcontroller.presentation.RobotControlScreen
import com.gonzalo.robotcontroller.presentation.RobotControlViewModel
import com.gonzalo.robotcontroller.presentation.RobotControlViewModelFactory
import com.gonzalo.robotcontroller.ui.theme.RobotControllerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RobotControlViewModel by viewModels {
        val webSocketClient = WebSocketClient()
        val repository = RobotRepository(webSocketClient, lifecycleScope)
        val settingsDataStore = SettingsDataStore(applicationContext)
        RobotControlViewModelFactory(repository, settingsDataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RobotControllerTheme {
                val connectionState by viewModel.connectionState.collectAsState()

                RobotControlScreen(
                    connectionState = connectionState,
                    onConnect = { viewModel.connect() },
                    onDisconnect = { viewModel.disconnect() },
                    onSendCommand = { command -> viewModel.sendCommand(command) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}