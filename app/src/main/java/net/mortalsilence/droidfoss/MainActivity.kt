package net.mortalsilence.droidfoss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.mortalsilence.droidfoss.comm.Mode

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


        Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            ) {
                Row {
                    Button(onClick =  { mainViewModel.fetchMode() }) {
                        Text(text = "Read mode")
                    }
                    Text(text = mainState.mode.name)
                }
                Row {
                    Button(onClick = { mainViewModel.setMode(Mode.DEMAND) }) {
                        Text(text = "DEMAND")
                    }
                    Button(onClick = { mainViewModel.setMode(Mode.MANUAL) }) {
                        Text(text = "MANUAL")
                    }
                    Button(onClick = { mainViewModel.setMode(Mode.PROGRAM) }) {
                        Text(text = "PROGRAM")
                    }
                    Button(onClick = { mainViewModel.setMode(Mode.OFF) }) {
                        Text(text = "OFF")
                    }
                }
            }
        }

        mainState.snackbarMessages.let { messages ->
            LaunchedEffect(messages) {
                messages.forEach{
                    snackbarHostState.showSnackbar(it)
                    mainViewModel.markMessageShown(it)
                }
            }
        }
    }


}