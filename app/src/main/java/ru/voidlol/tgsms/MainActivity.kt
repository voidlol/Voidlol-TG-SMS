package ru.voidlol.tgsms

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import java.io.File
import ru.voidlol.tgsms.ui.theme.StatusGreen
import ru.voidlol.tgsms.ui.theme.StatusAmber
import ru.voidlol.tgsms.ui.theme.VoidlolTGSMSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.voidlol.tgsms.data.AppSettings
import ru.voidlol.tgsms.data.AppSettingsStore
import ru.voidlol.tgsms.service.RelayMonitorWorker
import ru.voidlol.tgsms.service.RelayService
import ru.voidlol.tgsms.telegram.TelegramSender
import ru.voidlol.tgsms.update.AppUpdateInfo
import ru.voidlol.tgsms.update.AppUpdateStateStore
import ru.voidlol.tgsms.update.AppUpdater
import ru.voidlol.tgsms.update.AppUpdateWorker
import ru.voidlol.tgsms.util.DeviceInfoFormatter
import ru.voidlol.tgsms.util.TestMessageFormatter

class MainActivity : ComponentActivity() {
    private lateinit var appUpdater: AppUpdater
    private lateinit var appUpdateStateStore: AppUpdateStateStore
    private lateinit var installPermissionLauncher: ActivityResultLauncher<Intent>
    private var pendingInstallApk: File? = null
    private var updateUiState by mutableStateOf(AppUpdateUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = AppSettingsStore(applicationContext)
        appUpdater = AppUpdater(applicationContext)
        appUpdateStateStore = AppUpdateStateStore(applicationContext)
        AppUpdateWorker.schedule(applicationContext)
        RelayMonitorWorker.schedule(applicationContext)
        RelayService.ensureRunning(applicationContext)
        val availableUpdate = appUpdateStateStore.loadAvailableUpdate()
        updateUiState = AppUpdateUiState(
            installedVersionName = appUpdater.currentVersionName(),
            availableUpdate = availableUpdate,
            statusMessage = if (availableUpdate != null) {
                getString(
                    R.string.update_available_message,
                    availableUpdate.versionName
                )
            } else {
                getString(R.string.update_idle_message)
            }
        )
        installPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            pendingInstallApk?.let(::attemptInstallDownloadedUpdate)
        }

        setContent {
            VoidlolTGSMSTheme {
                TelegramForwarderScreen(
                    initialSettings = store.load(),
                    updateState = updateUiState,
                    onSave = { settings ->
                        store.save(settings)
                        toast(getString(R.string.settings_saved))
                    },
                    onSendTest = { settings, onFinished ->
                        lifecycleScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                TelegramSender.sendMessage(
                                    settings = settings,
                                    message = TestMessageFormatter.format(
                                        DeviceInfoFormatter.appName(this@MainActivity)
                                    )
                                )
                            }
                            onFinished(result)
                        }
                    },
                    onUpdateAction = {
                        pendingInstallApk?.let(::attemptInstallDownloadedUpdate)
                            ?: updateUiState.availableUpdate?.let(::downloadAndInstallUpdate)
                    }
                )
            }
        }

        refreshAvailableUpdate()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun refreshAvailableUpdate() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { appUpdater.checkForUpdate() }
            }
            val updateInfo = result.getOrElse { return@launch }

            appUpdateStateStore.saveAvailableUpdate(updateInfo)
            updateUiState = updateUiState.copy(
                availableUpdate = updateInfo,
                statusMessage = if (updateInfo != null) {
                    getString(R.string.update_available_message, updateInfo.versionName)
                } else {
                    getString(R.string.update_idle_message)
                },
                errorMessage = null
            )
        }
    }

    private fun downloadAndInstallUpdate(updateInfo: AppUpdateInfo) {
        updateUiState = updateUiState.copy(
            isDownloading = true,
            downloadProgress = 0,
            errorMessage = null,
            statusMessage = getString(R.string.update_download_starting, updateInfo.versionName)
        )

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    appUpdater.downloadUpdate(updateInfo) { progress ->
                        runOnUiThread {
                            updateUiState = updateUiState.copy(downloadProgress = progress)
                        }
                    }
                }
            }

            result
                .onSuccess { apkFile ->
                    pendingInstallApk = apkFile
                    updateUiState = updateUiState.copy(
                        isDownloading = false,
                        downloadProgress = 100,
                        hasDownloadedApk = true,
                        errorMessage = null,
                        statusMessage = getString(R.string.update_download_ready, updateInfo.versionName)
                    )
                    appUpdateStateStore.saveAvailableUpdate(updateInfo)
                    attemptInstallDownloadedUpdate(apkFile)
                }
                .onFailure { error ->
                    pendingInstallApk = null
                    updateUiState = updateUiState.copy(
                        isDownloading = false,
                        downloadProgress = null,
                        hasDownloadedApk = false,
                        errorMessage = error.message ?: getString(R.string.update_download_failed_generic),
                        statusMessage = null
                    )
                    toast(
                        getString(
                            R.string.update_download_failed,
                            error.message ?: getString(R.string.update_download_failed_generic)
                        )
                    )
                }
        }
    }

    private fun attemptInstallDownloadedUpdate(apkFile: File) {
        pendingInstallApk = apkFile

        if (!appUpdater.canRequestPackageInstalls()) {
            updateUiState = updateUiState.copy(
                hasDownloadedApk = true,
                statusMessage = getString(R.string.update_enable_installs),
                errorMessage = null
            )
            installPermissionLauncher.launch(appUpdater.createUnknownSourcesSettingsIntent())
            return
        }

        try {
            startActivity(appUpdater.createInstallIntent(apkFile))
            updateUiState = updateUiState.copy(
                hasDownloadedApk = true,
                statusMessage = getString(R.string.update_install_prompted),
                errorMessage = null
            )
        } catch (_: ActivityNotFoundException) {
            updateUiState = updateUiState.copy(
                hasDownloadedApk = true,
                statusMessage = null,
                errorMessage = getString(R.string.update_installer_unavailable)
            )
        }
    }
}

// ── Main screen ────────────────────────────────────────────

@Composable
private fun TelegramForwarderScreen(
    initialSettings: AppSettings,
    updateState: AppUpdateUiState,
    onSave: (AppSettings) -> Unit,
    onSendTest: (AppSettings, (Result<Unit>) -> Unit) -> Unit,
    onUpdateAction: () -> Unit
) {
    val context = LocalContext.current
    var botToken by rememberSaveable { mutableStateOf(initialSettings.botToken) }
    var chatId by rememberSaveable { mutableStateOf(initialSettings.chatId) }
    var batteryAlertThreshold by rememberSaveable {
        mutableStateOf(initialSettings.batteryAlertThresholdPercent.toFloat())
    }
    var isSending by remember { mutableStateOf(false) }
    var serviceRunning by remember { mutableStateOf(RelayService.shouldRun(context) && RelayService.isRunning) }

    val permissions = remember {
        listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_NUMBERS
        )
    }

    val permissionStates = permissions.associateWith { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    val currentSettings = AppSettings(
        botToken = botToken,
        chatId = chatId,
        batteryAlertThresholdPercent = batteryAlertThreshold.toInt()
    )
    val allGranted = permissionStates.values.all { it }
    val notificationsGranted = areNotificationsEnabled(context)
    val batteryOptimizationIgnored = isIgnoringBatteryOptimizations(context)
    val manufacturer = Build.MANUFACTURER.orEmpty()
    val isHonorFamilyDevice =
        manufacturer.equals("HONOR", ignoreCase = true) ||
            manufacturer.equals("HUAWEI", ignoreCase = true)

    LaunchedEffect(Unit) {
        RelayService.ensureRunning(context)
        serviceRunning = RelayService.shouldRun(context) && RelayService.isRunning
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ─────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "TG-SMS",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "SMS relay to Telegram",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Status card ────────────────────────────
            StatusCard(allGranted = allGranted, isConfigured = currentSettings.isComplete)

            // ── Update card ────────────────────────────
            UpdateCard(
                updateState = updateState,
                onUpdateAction = onUpdateAction
            )

            // ── Telegram config card ───────────────────
            SectionCard(
                title = "Telegram endpoint",
                icon = Icons.AutoMirrored.Outlined.Message
            ) {
                OutlinedTextField(
                    value = botToken,
                    onValueChange = { botToken = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.bot_token_label)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = chatId,
                    onValueChange = { chatId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.chat_id_label)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Tag,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(
                        R.string.battery_alert_threshold_label,
                        batteryAlertThreshold.toInt()
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.battery_alert_threshold_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                BatteryThresholdControl(
                    value = batteryAlertThreshold,
                    onValueChange = { batteryAlertThreshold = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onSave(currentSettings) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.save_settings),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                FilledTonalButton(
                    onClick = {
                        if (!currentSettings.isComplete) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_missing),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@FilledTonalButton
                        }
                        isSending = true
                        onSendTest(currentSettings) { result ->
                            isSending = false
                            val toastMessage = if (result.isSuccess) {
                                context.getString(R.string.test_sent)
                            } else {
                                context.getString(
                                    R.string.test_failed,
                                    result.exceptionOrNull()?.message.orEmpty()
                                )
                            }
                            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSending,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSending) "Sending..." else stringResource(R.string.send_test),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // ── Permissions card ───────────────────────
            SectionCard(
                title = "Permissions",
                icon = Icons.Outlined.Security
            ) {
                PermissionRow(
                    label = "Receive SMS",
                    icon = Icons.Outlined.Sms,
                    granted = permissionStates[Manifest.permission.RECEIVE_SMS] == true
                )
                PermissionRow(
                    label = "Phone state",
                    icon = Icons.Outlined.PhoneAndroid,
                    granted = permissionStates[Manifest.permission.READ_PHONE_STATE] == true
                )
                PermissionRow(
                    label = "Contacts",
                    icon = Icons.Outlined.ContactPhone,
                    granted = permissionStates[Manifest.permission.READ_CONTACTS] == true
                )
                PermissionRow(
                    label = "Phone numbers",
                    icon = Icons.Outlined.PhoneAndroid,
                    granted = permissionStates[Manifest.permission.READ_PHONE_NUMBERS] == true
                )

                if (!allGranted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.permissions_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { permissionLauncher.launch(permissions.toTypedArray()) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.grant_permissions),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // ── Relay service card ────────────────────
            SectionCard(
                title = stringResource(R.string.relay_service_title),
                icon = Icons.Outlined.Sync
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulseDot(
                        color = if (serviceRunning) StatusGreen else MaterialTheme.colorScheme.outline,
                        active = serviceRunning
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (serviceRunning) {
                            stringResource(R.string.relay_running)
                        } else {
                            stringResource(R.string.relay_stopped)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!notificationsGranted || !batteryOptimizationIgnored || isHonorFamilyDevice) {
                    Text(
                        text = stringResource(R.string.relay_background_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!notificationsGranted) {
                        FilledTonalButton(
                            onClick = { openAppNotificationSettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.relay_open_notification_settings),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!batteryOptimizationIgnored) {
                        FilledTonalButton(
                            onClick = { openBatteryOptimizationSettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.relay_open_battery_settings),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (isHonorFamilyDevice) {
                        Text(
                            text = stringResource(R.string.relay_honor_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (serviceRunning) {
                    FilledTonalButton(
                        onClick = {
                            RelayService.stop(context)
                            serviceRunning = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.relay_stop),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (!currentSettings.isComplete) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.relay_needs_setup),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@Button
                            }
                            RelayService.start(context)
                            serviceRunning = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.relay_start),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // ── Capabilities card ──────────────────────
            SectionCard(
                title = "Capabilities",
                icon = Icons.Outlined.BatteryAlert
            ) {
                CapabilityRow(label = "SMS forwarding", value = "Incoming messages")
                CapabilityRow(
                    label = "Battery alerts",
                    value = "Starts at ${currentSettings.batteryAlertThresholdPercent}% and repeats every 2% lower"
                )
                CapabilityRow(label = "Contact lookup", value = "Sender name resolution")
                CapabilityRow(label = "Bot commands", value = "/ping /status /log /help")
                CapabilityRow(label = "Boot auto-start", value = "Survives reboots")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun areNotificationsEnabled(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }

    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun isIgnoringBatteryOptimizations(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return true
    }

    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBatteryOptimizationSettings(context: android.content.Context) {
    val packageUri = Uri.parse("package:${context.packageName}")
    val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching { context.startActivity(requestIntent) }
        .recoverCatching { context.startActivity(fallbackIntent) }
}

private fun openAppNotificationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

// ── Status card ────────────────────────────────────────────

@Composable
private fun StatusCard(allGranted: Boolean, isConfigured: Boolean) {
    val isReady = allGranted && isConfigured

    val containerColor by animateColorAsState(
        targetValue = when {
            isReady -> StatusGreen.copy(alpha = 0.12f)
            isConfigured || allGranted -> StatusAmber.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
        },
        label = "statusBg"
    )

    val accentColor = when {
        isReady -> StatusGreen
        isConfigured || allGranted -> StatusAmber
        else -> MaterialTheme.colorScheme.error
    }

    val statusText = when {
        isReady -> "Ready to relay"
        !isConfigured && !allGranted -> "Setup required"
        !isConfigured -> "Enter Telegram credentials"
        else -> "Grant permissions"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PulseDot(color = accentColor, active = isReady)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor
                )
                Text(
                    text = if (isReady) "All systems operational" else "Complete setup below",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = if (isReady) Icons.Outlined.CheckCircle else Icons.Outlined.Error,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ── Pulse dot ──────────────────────────────────────────────

@Composable
private fun PulseDot(color: androidx.compose.ui.graphics.Color, active: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .alpha(if (active) alpha else 1f)
            .clip(CircleShape)
            .background(color)
    )
}

// ── Section card ───────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

data class AppUpdateUiState(
    val installedVersionName: String = "",
    val availableUpdate: AppUpdateInfo? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int? = null,
    val hasDownloadedApk: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@Composable
private fun UpdateCard(
    updateState: AppUpdateUiState,
    onUpdateAction: () -> Unit
) {
    SectionCard(
        title = stringResource(R.string.update_title),
        icon = Icons.Outlined.Sync
    ) {
        Text(
            text = stringResource(R.string.update_installed_version, updateState.installedVersionName),
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            updateState.availableUpdate != null -> {
                Text(
                    text = stringResource(
                        R.string.update_available_message,
                        updateState.availableUpdate.versionName
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = updateState.availableUpdate.changelog.ifBlank {
                        stringResource(R.string.update_default_changelog)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            updateState.errorMessage != null -> {
                Text(
                    text = updateState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> {
                Text(
                    text = updateState.statusMessage ?: stringResource(R.string.update_idle_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (updateState.isDownloading) {
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { ((updateState.downloadProgress ?: 0) / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.update_download_progress,
                    updateState.downloadProgress ?: 0
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (!updateState.statusMessage.isNullOrBlank() && updateState.availableUpdate != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = updateState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (updateState.availableUpdate != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onUpdateAction,
                enabled = !updateState.isDownloading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = when {
                        updateState.isDownloading -> stringResource(R.string.update_downloading)
                        updateState.hasDownloadedApk -> stringResource(R.string.update_install_button)
                        else -> stringResource(R.string.update_download_button)
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.update_schedule_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BatteryThresholdControl(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val threshold = value.toInt()
    val accentColor = when {
        threshold <= 20 -> MaterialTheme.colorScheme.error
        threshold <= 35 -> StatusAmber
        else -> MaterialTheme.colorScheme.primary
    }
    val nextThresholds = listOf(
        threshold,
        (threshold - 2).coerceAtLeast(0),
        (threshold - 4).coerceAtLeast(0)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.battery_alert_preview_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.battery_alert_threshold_value, threshold),
                        style = MaterialTheme.typography.headlineMedium,
                        color = accentColor
                    )
                }

                StepperButton(
                    icon = Icons.Outlined.Remove,
                    enabled = threshold > 10,
                    onClick = { onValueChange((threshold - 1).coerceAtLeast(10).toFloat()) }
                )
                Spacer(modifier = Modifier.width(10.dp))
                StepperButton(
                    icon = Icons.Outlined.Add,
                    enabled = threshold < 60,
                    onClick = { onValueChange((threshold + 1).coerceAtMost(60).toFloat()) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 10f..60f,
                steps = 49,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.surface,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    activeTickColor = accentColor,
                    inactiveTickColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "10%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "60%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                nextThresholds.forEach { ThresholdHintChip(text = "$it%") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(
                    R.string.battery_alert_preview_hint,
                    nextThresholds[0],
                    nextThresholds[1],
                    nextThresholds[2]
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThresholdHintChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Permission row ─────────────────────────────────────────

@Composable
private fun PermissionRow(label: String, icon: ImageVector, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = if (granted) "Granted" else "Not granted",
            tint = if (granted) StatusGreen else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp)
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
}

// ── Capability row ─────────────────────────────────────────

@Composable
private fun CapabilityRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
}
