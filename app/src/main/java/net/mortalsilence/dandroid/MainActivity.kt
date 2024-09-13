package net.mortalsilence.dandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState.Indeterminate
import androidx.compose.ui.state.ToggleableState.Off
import androidx.compose.ui.state.ToggleableState.On
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType.Companion.Em
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import net.mortalsilence.dandroid.comm.Mode
import net.mortalsilence.dandroid.data.AirUnitState
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import kotlin.enums.EnumEntries

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val bigLabelFontSize = TextUnit(4f, Em)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Content()
        }
    }

    @Composable
    private fun Content(mainViewModel: MainViewModel = viewModel()) {
        val snackbarHostState = remember { SnackbarHostState() }
        val mainState by mainViewModel::airUnitState
        val snackbarMessage by mainViewModel::snackbarMessage
        val isRefreshing by mainViewModel::isRefreshing
        var currentScreen by remember { mutableStateOf("airunitstate") }
        val scrollState = rememberScrollState()

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                BottomAppBar(actions = {
                    IconButton(onClick = { currentScreen = "airunitstate" }) {
                        Icon(Icons.Filled.Home, contentDescription = "Air unit state")
                    }
                    IconButton(onClick = { currentScreen = "airunitsettings" }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Air unit settings")
                    }
                    IconButton(onClick = { currentScreen = "preferences" }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Preferences")
                    }
                })
            }
        ) { paddingValues ->

            when (currentScreen) {
                "airunitstate" -> HomeScreen(
                    isRefreshing,
                    mainViewModel,
                    scrollState,
                    mainState
                )

                "airunitsettings" -> AirUnitSettingsScreen(mainViewModel, mainState, paddingValues)
                "preferences" -> PreferencesScreen(mainViewModel, paddingValues)
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
    private fun PreferencesScreen(mainViewModel: MainViewModel, paddingValues: PaddingValues) {

        val preferences by mainViewModel::preferences

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp)
                .padding(16.dp),
        ) {
            Text(
                "IP address (e.g. 192.168.0.7 )",
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically ) {
                OutlinedTextField(
                    value = preferences.ipAddress,
                    onValueChange = { mainViewModel.setIpAddress(it) },
                )
                if (preferences.ipValid) {
                    Icon(Icons.Filled.Check, contentDescription = "valid")
                } else {
                    Icon(Icons.Filled.Close, contentDescription = "invalid")
                }
            }
        }
    }

    @Composable
    private fun AirUnitSettingsScreen(
        mainViewModel: MainViewModel,
        mainState: AirUnitState,
        paddingValues: PaddingValues
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp)
                .padding(16.dp),
        ) {
            ChoiceRow("Mode", Mode.entries, getter = { mainState.mode },
                setter = { mainViewModel.setMode(it) })
            SwitchRow(
                "Boost",
                getter = { mainState.boost },
                setter = { mainViewModel.setBoost(it) })
            SwitchRow(
                "Night cooling",
                getter = { mainState.nightCooling },
                setter = { mainViewModel.setNightCooling(it) }
            )
            SwitchRow(
                "Bypass",
                getter = { mainState.bypass },
                setter = { mainViewModel.setBypass(it) }
            )
            SliderRow(
                "Manual Fan Step",
                { mainState.manualFanStep },
                { mainViewModel.setManualFanStep(it) },
                100,
                10
            )
        }
    }

    @Composable
    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    private fun HomeScreen(
        isRefreshing: Boolean,
        mainViewModel: MainViewModel,
        scrollState: ScrollState,
        mainState: AirUnitState
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { mainViewModel.fetchData() }) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxSize()
                    .padding(top = 30.dp)
                    .padding(16.dp),
            ) {
                FlowRow(
                    modifier = Modifier.padding(16.dp)
                ) {
                    DataFieldTile(stringResource(R.string.label_unit_name), mainState.unitName)
                    DataFieldTile(
                        stringResource(R.string.label_unit_serial),
                        mainState.unitSerialNo
                    )
                    DataFieldTile(
                        stringResource(R.string.label_supply_fan_speed),
                        mainState.supplyFanSpeed
                    )
                    DataFieldTile(
                        stringResource(R.string.label_extract_fan_speed),
                        mainState.extractFanSpeed
                    )
                    DataFieldTile(stringResource(R.string.label_filter_life), mainState.filterLife)
                    DataFieldTile(
                        stringResource(R.string.label_filter_period),
                        mainState.filterPeriod
                    )
                    DataFieldTile(
                        stringResource(R.string.label_supply_fan_step),
                        mainState.supplyFanStep
                    )
                    DataFieldTile(
                        stringResource(R.string.label_extract_fan_step),
                        mainState.extractFanStep
                    )
                    DataFieldTile(
                        stringResource(R.string.label_room_temperature),
                        mainState.roomTemp
                    )
                    DataFieldTile(
                        stringResource(R.string.label_room_temperature_calculated),
                        mainState.roomTempCalculated
                    )
                    DataFieldTile(
                        stringResource(R.string.label_outdoor_temperature),
                        mainState.outdoorTemp
                    )
                    DataFieldTile("Supply temperature", mainState.supplyTemp)
                    DataFieldTile("Extract temperature", mainState.extractTemp)
                    DataFieldTile("Exhaust temperature", mainState.exhaustTemp)
                    DataFieldTile("Battery life (remaining)", mainState.batteryLife)
                    DataFieldTile(
                        "Time", mainState.currentTime?.format(ISO_LOCAL_DATE_TIME)
                    )
                }
            }
        }
    }

    @Composable
    private fun DataFieldTile(title: String, value: Any?) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .clip(RoundedCornerShape(20))
                .background(color = LightGray)
                .padding(6.dp)
        ) {
            Column {
                Text(title, fontSize = TextUnit(3f, Em))
                Text(
                    text = value?.toString() ?: "N/A",
                    fontSize = TextUnit(4f, Em)
                )
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$title: ",
                modifier = Modifier.padding(end = 16.dp),
                fontSize = bigLabelFontSize
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

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun <T : Enum<T>> ChoiceRow(
        title: String,
        choices: EnumEntries<T>,
        getter: () -> T,
        setter: (T) -> Unit
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$title: ",
                modifier = Modifier.padding(end = 16.dp),
                fontSize = bigLabelFontSize
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$title: ",
                modifier = Modifier.padding(end = 16.dp),
                fontSize = bigLabelFontSize
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