# Voidlol TG-SMS

An Android app that silently forwards incoming SMS messages and battery alerts to a Telegram chat via a bot. Designed for unattended operation — leave a phone with your SIM cards on a charger and receive all SMS on your main device via Telegram. All credentials stay on-device — no cloud backend, no sign-up.

## Features

- **SMS Forwarding** — Intercepts incoming SMS and relays them to Telegram in real time
- **Battery Alerts** — Sends a notification to Telegram when the device battery drops to a low level
- **Contact Resolution** — Resolves sender phone numbers to contact names when available
- **SIM Identification** — Tags messages with the receiving SIM's phone number
- **Telegram Bot Commands** — Control and monitor the relay remotely via `/ping`, `/status`, `/log`, `/help`
- **Boot Auto-Start** — Foreground service and bot polling restart automatically after reboots and app updates
- **Foreground Service** — Lightweight persistent service keeps the relay alive and enables bot command polling via Telegram long-polling
- **Persistent Message Queue** — Messages are saved to disk immediately and survive app crashes, reboots, and battery drain; delivered automatically when connectivity is restored
- **Offline Resilience** — Uses WorkManager with network constraints — no battery wasted on failed requests when offline
- **Encrypted Storage** — Bot token and chat ID are stored locally via EncryptedSharedPreferences (AES-256)
- **Connection Pooling** — OkHttp client reuses connections to minimize TLS overhead
- **Test Message** — Send a test message from the app to verify your Telegram setup
- **Material 3 UI** — Clean, modern interface with light/dark theme support and animated status indicators

## Screenshots

<!-- Add screenshots here -->

## Requirements

- Android 7.0+ (API 24)
- A Telegram bot token ([create one via @BotFather](https://t.me/BotFather))
- Your Telegram chat ID ([find it via @userinfobot](https://t.me/userinfobot))

## Setup

1. Install the app (download the APK from [Releases](../../releases) or build from source)
2. Open the app and enter your **Bot Token** and **Chat ID**
3. Tap **Save Settings**
4. Grant the requested permissions:
   - `RECEIVE_SMS` / `READ_SMS` — to intercept incoming messages
   - `READ_PHONE_STATE` — to identify the receiving SIM
   - `READ_PHONE_NUMBERS` — to read the SIM phone number
   - `READ_CONTACTS` — to resolve sender names from your contacts
5. Tap **Send Test** to verify delivery
6. Tap **Start Relay** to enable the foreground service and bot commands
7. Done — the app works in the background, survives reboots, no need to keep it open

## Bot Commands

Send these commands to your bot in the configured Telegram chat:

| Command   | Description                                      |
|-----------|--------------------------------------------------|
| `/ping`   | Check if the relay is alive (returns timestamp)  |
| `/status` | Battery %, charging source, network, uptime, pending queue, device info |
| `/log`    | Last 10 forwarded messages with relative timestamps |
| `/help`   | List available commands                          |

Example `/status` response:
```
TG-SMS Relay Status
---
Battery: 85% (charging via AC)
Network: WiFi
Uptime: 3d 14h 22m
Pending: 0 message(s)
Device: Samsung SM-S911B
Android: 14 (API 34)
```

## Message Format

**SMS:**
```
[+1234567890][John Doe] Hey, are you coming tonight?
```

**Battery alert:**
```
[Pixel 8][battery] Low battery alert: 15%
```

**Test message:**
```
[Voidlol TG-SMS][test] Telegram delivery is working
```

## Architecture

```
SMS arrives → SmsReceiver formats message (local, fast)
           → MessageQueueStore.add() writes JSON to disk (atomic)
           → WorkManager schedules TelegramWorker (requires network)

Bot command → RelayService long-polls Telegram getUpdates (60s timeout)
           → BotCommandHandler processes /ping, /status, /log, /help
           → Response sent directly via TelegramSender

Device offline?  → Messages persist in pending_messages.json
Device reboots?  → BootReceiver restarts RelayService + flushes queue
App crashes?     → Already on disk — nothing lost
Phone dies?      → Already on disk — sends when charged + online
```

## Building from Source

### Prerequisites

- Android Studio or JDK 17+
- Android SDK with API 36

### Build

```bash
git clone https://github.com/your-username/VoidlolTGSMS.git
cd VoidlolTGSMS
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Signed Release Build

1. Generate a keystore:
```bash
keytool -genkey -v -keystore release.keystore -alias my-key -keyalg RSA -keysize 2048 -validity 10000
```
2. Place `release.keystore` in the project root
3. Build with signing credentials:
```bash
KEYSTORE_PASSWORD=your_password KEY_ALIAS=my-key KEY_PASSWORD=your_password ./gradlew assembleRelease
```

## CI/CD

The project includes a GitHub Actions workflow (`.github/workflows/release.yml`) that automatically builds and publishes a signed release APK on every push to master.

### Required GitHub Secrets

| Secret             | Description                                        |
|--------------------|----------------------------------------------------|
| `KEYSTORE_BASE64`  | Base64-encoded keystore (`base64 -i release.keystore`) |
| `KEYSTORE_PASSWORD`| Keystore password                                  |
| `KEY_ALIAS`        | Key alias                                          |
| `KEY_PASSWORD`     | Key password                                       |

## Project Structure

```
app/src/main/java/ru/voidlol/tgsms/
├── MainActivity.kt            # Compose UI — settings, permissions, relay toggle
├── RelayService.kt            # Foreground service — keeps app alive, hosts bot poller
├── BootReceiver.kt            # BOOT_COMPLETED + MY_PACKAGE_REPLACED auto-start
├── SmsReceiver.kt             # BroadcastReceiver for incoming SMS
├── BatteryAlertReceiver.kt    # BroadcastReceiver for low battery
├── TelegramBotPoller.kt       # Long-polling loop for Telegram bot commands
├── BotCommandHandler.kt       # /ping, /status, /log, /help command handlers
├── TelegramSender.kt          # OkHttp client for Telegram Bot API
├── TelegramWorker.kt          # WorkManager worker — drains message queue
├── MessageQueue.kt            # Facade — enqueue message + schedule worker
├── MessageQueueStore.kt       # Persistent JSON file queue (crash-safe)
├── ForwardedMessageLog.kt     # Log of last 30 sent messages (for /log command)
├── PendingMessage.kt          # Data class for queued messages
├── AppSettings.kt             # Data class for bot token & chat ID
├── AppSettingsStore.kt        # EncryptedSharedPreferences persistence
├── MessageFormatter.kt        # SMS message formatting
├── BatteryAlertFormatter.kt   # Battery alert formatting
├── BatteryStatusReader.kt     # Battery percentage extraction
├── DeviceInfoFormatter.kt     # Device manufacturer/model
├── PhoneMetadataResolver.kt   # SIM number & contact name lookup
├── TestMessageFormatter.kt    # Test message formatting
└── ui/theme/
    ├── Color.kt               # Color palette (light & dark)
    ├── Theme.kt               # Material 3 theme configuration
    └── Type.kt                # Typography definitions
```

## Permissions

| Permission                          | Purpose                              |
|-------------------------------------|--------------------------------------|
| `INTERNET`                          | Send messages to Telegram Bot API    |
| `RECEIVE_SMS`                       | Intercept incoming SMS               |
| `READ_SMS`                          | Read SMS message content             |
| `READ_PHONE_STATE`                  | Identify the receiving SIM           |
| `READ_PHONE_NUMBERS`                | Read SIM phone number                |
| `READ_CONTACTS`                     | Resolve sender names from contacts   |
| `RECEIVE_BOOT_COMPLETED`            | Auto-start relay after reboot        |
| `FOREGROUND_SERVICE`                | Keep relay service running           |
| `FOREGROUND_SERVICE_REMOTE_MESSAGING` | Required for Android 14+ foreground service |

## Privacy

- No data leaves your device except the SMS content sent to your own Telegram bot
- No analytics, no tracking, no third-party services
- Bot token and chat ID are encrypted on-device via EncryptedSharedPreferences
- Credentials are excluded from Android cloud backup
- Bot commands are only accepted from the configured chat ID

## License

MIT
