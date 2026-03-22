# Voidlol TG-SMS

An Android app that forwards incoming SMS messages and battery alerts to a Telegram chat via a bot. It is designed for unattended operation: leave a phone with your SIM cards on a charger and receive all SMS on your main device through Telegram. All credentials stay on-device, and APK updates are distributed directly through GitHub Releases plus GitHub Pages metadata.

## Features

- **SMS Forwarding** — Intercepts incoming SMS and relays them to Telegram in real time
- **Battery Alerts** — Sends a notification to Telegram when the device battery drops to a low level
- **Contact Resolution** — Resolves sender phone numbers to contact names when available
- **SIM Identification** — Tags messages with the receiving SIM's phone number
- **Telegram Bot Commands** — Control and monitor the relay remotely via `/ping`, `/status`, `/log`, `/help`
- **Boot Auto-Start** — Foreground service and bot polling restart automatically after reboots and app updates
- **Daily Update Checks** — The app checks once per day for a newer APK published to GitHub Releases
- **In-App APK Updates** — When a newer version is found, the app shows it in the UI, posts a notification, downloads the APK, and opens the Android installer
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
- If you want update notifications on Android 13+, allow notifications for the app

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
7. Done — the app works in the background, survives reboots, and checks for app updates automatically once per day

## App Updates

The project uses GitHub Releases plus GitHub Pages for self-hosted APK updates.

- The release workflow builds a signed APK and publishes a GitHub Release
- The same workflow deploys `update.json` to GitHub Pages
- The app checks `https://voidlol.github.io/Voidlol-TG-SMS/update.json` once per day using `WorkManager`
- When a newer version is found, the app stores the metadata, posts a notification, and offers download/install inside the app

Current update metadata endpoint:

```text
https://voidlol.github.io/Voidlol-TG-SMS/update.json
```

Example payload:

```json
{
  "versionCode": 10005,
  "versionName": "1.0.5",
  "apkUrl": "https://github.com/voidlol/Voidlol-TG-SMS/releases/download/v1.0.5-10005-deadbee/VoidlolTGSMS-1.0.5.apk",
  "changelog": "See GitHub release notes",
  "publishedAt": "2026-03-22T12:00:00Z"
}
```

Important Android limitation:

- The app can auto-check and auto-download, but Android still requires user confirmation in the package installer
- On Android 8+, the user may also need to allow installs from this app before the APK installer can open

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

App update check → AppUpdateWorker fetches update.json once per day
                → New version stored locally + notification posted
                → MainActivity downloads APK and opens installer on demand

Device offline?  → Messages persist in pending_messages.json
Device reboots?  → BootReceiver restarts RelayService + reschedules update checks
App crashes?     → Already on disk — nothing lost
Phone dies?      → Already on disk — sends when charged + online
```

## Building from Source

### Prerequisites

- Android Studio or JDK 17+
- Android SDK with API 36

### Build

```bash
git clone https://github.com/voidlol/Voidlol-TG-SMS.git
cd Voidlol-TG-SMS
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

## Release Automation

The repository ships with a GitHub Actions release workflow that:

1. Bumps `version.properties`
2. Builds the signed release APK
3. Creates a GitHub Release and uploads the APK
4. Publishes `update.json` to GitHub Pages

For this to work correctly:

- Configure GitHub Pages to publish from `GitHub Actions`
- Add signing secrets used by the workflow:
  - `KEYSTORE_BASE64`
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`

## Project Structure

```
app/src/main/java/ru/voidlol/tgsms/
├── MainActivity.kt                # Compose UI — settings, permissions, relay toggle
├── data/
│   ├── AppSettings.kt             # Data class for bot token & chat ID
│   ├── AppSettingsStore.kt        # EncryptedSharedPreferences persistence
│   ├── ForwardedMessageLog.kt     # Log of last 30 sent messages (for /log command)
│   ├── MessageQueue.kt            # Facade — enqueue message + schedule worker
│   ├── MessageQueueStore.kt       # Persistent JSON file queue (crash-safe)
│   └── PendingMessage.kt          # Data class for queued messages
├── update/
│   ├── AppUpdateInfo.kt           # Update metadata model from update.json
│   ├── AppUpdateJsonParser.kt     # Validation/parsing for update.json
│   ├── AppUpdateStateStore.kt     # Persisted update metadata + notification state
│   ├── AppUpdater.kt              # APK download and installer handoff
│   └── AppUpdateWorker.kt         # Daily WorkManager job for update checks
├── receiver/
│   ├── BatteryAlertReceiver.kt    # BroadcastReceiver for low battery
│   ├── BootReceiver.kt            # BOOT_COMPLETED + MY_PACKAGE_REPLACED auto-start + update reschedule
│   └── SmsReceiver.kt             # BroadcastReceiver for incoming SMS
├── service/
│   ├── RelayService.kt            # Foreground service — keeps app alive, hosts bot poller
│   └── TelegramWorker.kt          # WorkManager worker — drains message queue
├── telegram/
│   ├── BotCommandHandler.kt       # /ping, /status, /log, /help command handlers
│   ├── TelegramBotPoller.kt       # Long-polling loop for Telegram bot commands
│   └── TelegramSender.kt          # OkHttp client for Telegram Bot API
├── ui/theme/
│   ├── Color.kt                   # Color palette (light & dark)
│   ├── Theme.kt                   # Material 3 theme configuration
│   └── Type.kt                    # Typography definitions
└── util/
    ├── BatteryAlertFormatter.kt   # Battery alert formatting
    ├── BatteryStatusReader.kt     # Battery percentage extraction
    ├── DeviceInfoFormatter.kt     # Device manufacturer/model
    ├── MessageFormatter.kt        # SMS message formatting
    ├── PhoneMetadataResolver.kt   # SIM number & contact name lookup
    └── TestMessageFormatter.kt    # Test message formatting
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
| `POST_NOTIFICATIONS`                | Show update notifications on Android 13+ |
| `REQUEST_INSTALL_PACKAGES`          | Open APK installer for self-updates  |

## Privacy

- No data leaves your device except the SMS content sent to your own Telegram bot
- No analytics, no tracking, no third-party services
- Bot token and chat ID are encrypted on-device via EncryptedSharedPreferences
- Credentials are excluded from Android cloud backup
- Bot commands are only accepted from the configured chat ID

## License

MIT
