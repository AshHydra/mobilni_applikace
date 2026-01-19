package cz.ash.mobilniapplikace.ui.screens

import android.Manifest
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import cz.ash.mobilniapplikace.data.settings.SettingsRepository
import cz.ash.mobilniapplikace.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Currency
import java.util.Locale
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    settingsRepository: SettingsRepository
) {
    val state by authViewModel.state.collectAsState()
    val user = state.user
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vsCurrency by settingsRepository.vsCurrencyFlow.collectAsState(initial = "usd")
    val useLocationCurrency by settingsRepository.useLocationCurrencyFlow.collectAsState(initial = false)

    var locationError by remember { mutableStateOf<String?>(null) }
    var isDetecting by remember { mutableStateOf(false) }
    val chipScroll = rememberScrollState()

    fun startDetection() {
        scope.launch {
            isDetecting = true
            locationError = null
            try {
                val currency = detectCurrencyFromLocation(context)
                settingsRepository.setVsCurrency(currency)
            } catch (t: Throwable) {
                locationError = t.message ?: "Failed to detect currency"
            } finally {
                isDetecting = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startDetection()
        } else {
            locationError = "Location permission denied"
            scope.launch { settingsRepository.setUseLocationCurrency(false) }
        }
    }

    LaunchedEffect(useLocationCurrency) {
        // If user enabled the switch, try to detect immediately (permission gating below).
        if (useLocationCurrency) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (granted) startDetection() else permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            // When turning off, we stop auto-detection and clear any previous error.
            locationError = null
            isDetecting = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("User", style = MaterialTheme.typography.titleLarge)
            Text("ID: ${user?.id ?: "—"}", modifier = Modifier.padding(top = 10.dp))
            Text("Email: ${user?.email ?: "—"}", modifier = Modifier.padding(top = 6.dp))

            Spacer(Modifier.height(22.dp))
            Text("Settings", style = MaterialTheme.typography.titleLarge)
            Text("Currency: ${vsCurrency.uppercase()}", modifier = Modifier.padding(top = 10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Use currency from my location",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = useLocationCurrency,
                    onCheckedChange = { checked ->
                        locationError = null
                        scope.launch {
                            if (checked) {
                                settingsRepository.enableLocationCurrency(vsCurrency)
                            } else {
                                settingsRepository.disableLocationCurrencyAndRestoreManual()
                            }
                        }
                    },
                    enabled = !isDetecting
                )
            }

            if (!useLocationCurrency) {
                Spacer(Modifier.height(12.dp))
                Text("Manual currency", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .horizontalScroll(chipScroll),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf("usd", "eur", "czk", "gbp", "inr")
                    options.forEach { code ->
                        FilterChip(
                            selected = vsCurrency.equals(code, ignoreCase = true),
                            onClick = {
                                scope.launch { settingsRepository.setManualCurrency(code) }
                            },
                            label = { Text(code.uppercase()) }
                        )
                    }
                }
            }

            if (locationError != null) {
                Text(
                    text = locationError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            if (state.error != null) {
                Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            OutlinedButton(
                onClick = { authViewModel.signOut() },
                enabled = !state.isLoading,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text(if (state.isLoading) "Signing out…" else "Sign out")
            }
        }
    }
}

private suspend fun detectCurrencyFromLocation(context: android.content.Context): String {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
        client.lastLocation
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    } ?: throw IllegalStateException("No last known location (turn on location and try again)")

    val geocoder = Geocoder(context, Locale.getDefault())
    val countryCode: String? = if (Build.VERSION.SDK_INT >= 33) {
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                cont.resume(addresses.firstOrNull()?.countryCode)
            }
        }
    } else {
        @Suppress("DEPRECATION")
        geocoder.getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()
            ?.countryCode
    }

    val cc = countryCode?.uppercase()
        ?: throw IllegalStateException("Could not resolve country from location")

    return runCatching { Currency.getInstance(Locale("", cc)).currencyCode.lowercase() }
        .getOrElse { throw IllegalStateException("Could not map country $cc to a currency") }
}

