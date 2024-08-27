package net.mortalsilence.droidfoss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.TextUnitType.Companion.Em
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.mortalsilence.droidfoss.comm.Mode
import kotlin.math.log10

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            content()
        }
    }

    @Composable
    private fun content(mainViewModel: MainViewModel = viewModel()) {
        val snackbarHostState = remember { SnackbarHostState() }
        val mainState by mainViewModel::mainState
        val scope = rememberCoroutineScope()

        Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            ) {
                Row(modifier = Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.padding(end = 16.dp)) {
                        Button(onClick = { mainViewModel.fetchData() }) {
                            Text(text = "Fetch AirUnit data")
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Mode", fontSize = TextUnit(3f, Em))
                        Text(text = mainState.mode.name, fontSize = TextUnit(4f, Em))
                        Text("Boost", fontSize = TextUnit(3f, Em))
                        Text(text = mainState.boost?.toString() ?: "UNKNOWN", fontSize = TextUnit(4f, Em))
                        Text("Supply Fan Speed", fontSize = TextUnit(3f, Em))
                        Text(text = mainState.supplyFanSpeed?.toString() ?: "UNKNOWN", fontSize = TextUnit(4f, Em))
                        Text("Extract Fan Speed", fontSize = TextUnit(3f, Em))
                        Text(text = mainState.extractFanSpeed?.toString() ?: "UNKNOWN", fontSize = TextUnit(4f, Em))
                    }

                }
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Mode: ",
                        modifier = Modifier.padding(end = 16.dp),
                        fontSize = TextUnit(6f, Em)
                    )
                    Button(
                        onClick = { mainViewModel.setMode(Mode.DEMAND) },
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Text(text = "DEMAND")
                    }
                    Button(
                        onClick = { mainViewModel.setMode(Mode.MANUAL) },
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Text(text = "MANUAL")
                    }
                    Button(
                        onClick = { mainViewModel.setMode(Mode.PROGRAM) },
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Text(text = "PROGRAM")
                    }
                    Button(
                        onClick = { mainViewModel.setMode(Mode.OFF) },
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Text(text = "OFF")
                    }
                }
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Boost: ",
                        modifier = Modifier.padding(end = 16.dp),
                        fontSize = TextUnit(6f, Em)
                    )
                    Button(
                        onClick = { mainViewModel.setBoost(true) },
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Text(text = "ON")
                    }
                    Button(
                        onClick = { mainViewModel.setBoost(false) },
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Text(text = "OFF")
                    }
                }
            }
        }

        mainState.snackbarMessage?.let { message ->
            LaunchedEffect(key1 = message) {
                snackbarHostState.showSnackbar(message)
                mainViewModel.markMessageShown(message)
            }
        }
    }


}