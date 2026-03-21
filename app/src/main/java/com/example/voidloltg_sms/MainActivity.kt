package com.example.voidloltg_sms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.voidloltg_sms.ui.theme.StatusGreen
import com.example.voidloltg_sms.ui.theme.StatusAmber
import com.example.voidloltg_sms.ui.theme.VoidlolTGSMSTheme
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = AppSettingsStore(applicationContext)
        setContent {
            VoidlolTGSMSTheme {
                TelegramForwarderScreen(
                    initialSettings = store.load(),
                    onSave = { settings ->
                        store.save(settings)
                        toast(getString(R.string.settings_saved))
                    },
                    onSendTest = { settings, onFinished ->
                        thread(name = "test-message") {
                            val result = TelegramSender.sendMessage(
                                settings = settings,
                                message = TestMessageFormatter.format(
                                    DeviceInfoFormatter.appName(this)
                                )
                            )
                            runOnUiThread {
                                onFinished(result)
                            }
                        }
                    }
                )
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// ── Main screen ────────────────────────────────────────────

@Composable
private fun TelegramForwarderScreen(
    initialSettings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onSendTest: (AppSettings, (Result<Unit>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var botToken by rememberSaveable { mutableStateOf(initialSettings.botToken) }
    var chatId by rememberSaveable { mutableStateOf(initialSettings.chatId) }
    var isSending by remember { mutableStateOf(false) }

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

    val currentSettings = AppSettings(botToken = botToken, chatId = chatId)
    val allGranted = permissionStates.values.all { it }

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

            // ── Telegram config card ───────────────────
            SectionCard(
                title = "Telegram endpoint",
                icon = Icons.AutoMirrored.Outlined.Message
            ) {
                OutlinedTextField(
                    value = botToken,
                    onValueChange = { botToken = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = context.getString(R.string.bot_token_label)) },
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
                    label = { Text(text = context.getString(R.string.chat_id_label)) },
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
                        text = context.getString(R.string.save_settings),
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
                            val message = if (result.isSuccess) {
                                context.getString(R.string.test_sent)
                            } else {
                                context.getString(
                                    R.string.test_failed,
                                    result.exceptionOrNull()?.message.orEmpty()
                                )
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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
                        text = if (isSending) "Sending..." else context.getString(R.string.send_test),
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
                        text = context.getString(R.string.permissions_hint),
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
                            text = context.getString(R.string.grant_permissions),
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
                CapabilityRow(label = "Battery alerts", value = "Low battery events")
                CapabilityRow(label = "Contact lookup", value = "Sender name resolution")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
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
