package net.mortalsilence.droidfoss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState.Indeterminate
import androidx.compose.ui.state.ToggleableState.Off
import androidx.compose.ui.state.ToggleableState.On
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType.Companion.Em
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import net.mortalsilence.droidfoss.comm.Mode
import java.time.format.DateTimeFormatter
import kotlin.enums.EnumEntries

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Content()
        }
    }

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable
    private fun Content(mainViewModel: MainViewModel = viewModel()) {
        val snackbarHostState = remember { SnackbarHostState() }
        val mainState by mainViewModel::airUnitState
        val snackbarMessage by mainViewModel::snackbarMessage
        val isRefreshing by mainViewModel::isRefreshing

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->

            val scrollState = rememberScrollState()

            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { mainViewModel.fetchData() }) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                ) {
                    FlowColumn(modifier = Modifier.padding(16.dp)) {
                        DataFieldTile("Unit name", mainState.unitName)
                        DataFieldTile("Unit serial no", mainState.unitSerialNo)
                        DataFieldTile("Mode", mainState.mode.name)
                        DataFieldTile("Boost", mainState.boost)
                        DataFieldTile("Supply Fan Speed", mainState.supplyFanSpeed)
                        DataFieldTile("Extract Fan Speed", mainState.extractFanSpeed)
                        DataFieldTile("Manual Fan Step", mainState.manualFanStep)
                        DataFieldTile("Filter Life", mainState.filterLife)
                        DataFieldTile("Filter Period", mainState.filterPeriod)
                        DataFieldTile("Supply Fan Speed", mainState.supplyFanStep)
                        DataFieldTile("Extract Fan Speed", mainState.extractFanStep)
                        DataFieldTile("Night Cooling", mainState.nightCooling)
                        DataFieldTile("Bypass", mainState.bypass)
                        DataFieldTile("Room temperature", mainState.roomTemp)
                        DataFieldTile("Room temperature (calculated)", mainState.roomTempCalculated)
                        DataFieldTile("Outdoor temperature", mainState.outdoorTemp)
                        DataFieldTile("Supply temperature", mainState.supplyTemp)
                        DataFieldTile("Extract temperature", mainState.extractTemp)
                        DataFieldTile("Exhaust temperature", mainState.exhaustTemp)
                        DataFieldTile("Battery life (remaining)", mainState.batteryLife)
                        DataFieldTile("Time", mainState.currentTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    }
                    ChoiceRow("Mode", Mode.entries, getter = { mainState.mode },
                        setter = { mainViewModel.setMode(it) })
                    SwitchRow(
                        "Boost",
                        getter = { mainState.boost },
                        setter = { mainViewModel.setBoost(it) })
                    SliderRow(
                        "Manual Fan Step",
                        { mainState.manualFanStep },
                        { mainViewModel.setManualFanStep(it) },
                        100,
                        10)

                }
            }
        }

        snackbarMessage?.let { message ->
            LaunchedEffect(key1 = message) {
                snackbarHostState.showSnackbar(message)
                mainViewModel.markMessageShown()
            }
        }
    }

    @Composable
    private fun SliderRow(
        title: String,
        getter: () -> Int?,
        setter: (Int) -> Unit,
        max: Int,
        steps: Int
    ) {
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$title: ",
                modifier = Modifier.padding(end = 16.dp),
                fontSize = TextUnit(6f, Em)
            )
            var sliderValueRaw by remember(getter) {
                mutableFloatStateOf(
                    getter.invoke()?.toFloat() ?: 0f
                )
            }
            Slider(
                value = sliderValueRaw,
                valueRange = 0f..max.toFloat(),
                steps = steps,
                onValueChange = { sliderValueRaw = it },
                onValueChangeFinished = { setter.invoke(sliderValueRaw.toInt()) }
            )
        }
    }

    @Composable
    private fun DataFieldTile(title: String, value: Any?) {
        Column {
            Text(title, fontSize = TextUnit(3f, Em))
            Text(
                text = value?.toString() ?: "N/A",
                fontSize = TextUnit(4f, Em),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun <T : Enum<T>> ChoiceRow(
        title: String,
        choices: EnumEntries<T>,
        getter: () -> T,
        setter: (T) -> Unit
    ) {
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$title: ",
                modifier = Modifier.padding(end = 16.dp),
                fontSize = TextUnit(6f, Em)
            )
            FlowRow(Modifier.selectableGroup()) {
                choices.forEach { choice ->
                    Row(
                        Modifier
                            .padding(4.dp)
                            .selectable(
                                selected = (choice == getter.invoke()),
                                onClick = { setter.invoke(choice) },
                                role = Role.RadioButton
                            )
                    ) {
                        RadioButton(selected = (choice == getter.invoke()), onClick = null)
                        Text(text = choice.name)
                    }
                }
            }
        }
    }

    @Composable
    private fun SwitchRow(
        title: String,
        getter: () -> Boolean?,
        setter: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$title: ",
                modifier = Modifier.padding(end = 16.dp),
                fontSize = TextUnit(6f, Em)
            )
            var stateRaw by remember(getter) {
                mutableStateOf(toggleableState(getter))
            }
            TriStateCheckbox(
                state = stateRaw,
                onClick = {
                    stateRaw = when (stateRaw) {
                        Indeterminate -> On
                        On -> Off
                        Off -> On
                    }
                    if (stateRaw != Indeterminate) setter.invoke(stateRaw == On)
                }
            )
        }
    }

    private fun toggleableState(getter: () -> Boolean?) = when (getter.invoke()) {
        true -> On
        false -> Off
        null -> Indeterminate
    }
}