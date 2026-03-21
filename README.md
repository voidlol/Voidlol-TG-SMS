# Voidlol TG-SMS                                                                                                                                                                                               
                                                                                                                                                                                                               
  An Android app that silently forwards incoming SMS messages and battery alerts to a Telegram chat via a bot. All credentials stay on-device — no cloud backend, no sign-up.                                    
   
  ## Features                                                                                                                                                                                                    
                                                                                                                                                                                                               
  - **SMS Forwarding** — Intercepts incoming SMS and relays them to Telegram in real time                                                                                                                        
  - **Battery Alerts** — Sends a notification to Telegram when the device battery drops to a low level
  - **Contact Resolution** — Resolves sender phone numbers to contact names when available                                                                                                                       
  - **SIM Identification** — Tags messages with the receiving SIM's phone number                                                                                                                                 
  - **On-Device Storage** — Bot token and chat ID are stored locally via SharedPreferences                                                                                                                       
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
     - `RECEIVE_SMS` — to intercept incoming messages                                                                                                                                                            
     - `READ_PHONE_STATE` — to identify the receiving SIM                                                                                                                                                        
     - `READ_PHONE_NUMBERS` — to read the SIM phone number                                                                                                                                                       
     - `READ_CONTACTS` — to resolve sender names from your contacts                                                                                                                                              
  5. Tap **Send Test** to verify delivery                                                                                                                                                                        
  6. Done — the app works in the background via broadcast receivers, no need to keep it open                                                                                                                     
                                                                                                                                                                                                                 
  ## Message Format                                                                                                                                                                                              
                                                                                                                                                                                                                 
  **SMS:**                                                                                                                                                                                                       
  [+1234567890][John Doe] Hey, are you coming tonight?
                                                                                                                                                                                                                 
  **Battery alert:**                                                                                                                                                                                           
  [Pixel 8][battery] Low battery alert: 15%                                                                                                                                                                      
                                                                                                                                                                                                                 
  **Test message:**
  [Voidlol TG-SMS][test] Telegram delivery is working                                                                                                                                                            
                                                                                                                                                                                                                 
  ## Building from Source
                                                                                                                                                                                                                 
  ### Prerequisites                                                                                                                                                                                            

  - Android Studio or JDK 17+                                                                                                                                                                                    
  - Android SDK with API 36
                                                                                                                                                                                                                 
  ### Build                                                                                                                                                                                                      
   
  ```bash                                                                                                                                                                                                        
  git clone https://github.com/your-username/VoidlolTGSMS.git                                                                                                                                                  
  cd VoidlolTGSMS
  ./gradlew assembleDebug

  The APK will be at app/build/outputs/apk/debug/app-debug.apk.                                                                                                                                                  
   
  Signed Release Build                                                                                                                                                                                           
                                                                                                                                                                                                               
  1. Generate a keystore:                                                                                                                                                                                        
  keytool -genkey -v -keystore release.keystore -alias my-key -keyalg RSA -keysize 2048 -validity 10000                                                                                                        
  2. Place release.keystore in the project root                                                                                                                                                                  
  3. Build with signing credentials:                                                                                                                                                                             
  KEYSTORE_PASSWORD=your_password KEY_ALIAS=my-key KEY_PASSWORD=your_password ./gradlew assembleRelease                                                                                                          
                                                                                                                                                                                                                 
  CI/CD                                                                                                                                                                                                          
                                                                                                                                                                                                               
  The project includes a GitHub Actions workflow (.github/workflows/release.yml) that automatically builds and publishes a signed release APK on every push to master.                                           
                                                                                                                                                                                                               
  Required GitHub Secrets                                                                                                                                                                                        
                                                                                                                                                                                                               
  ┌───────────────────┬──────────────────────────────────────────────────────┐                                                                                                                                   
  │      Secret       │                     Description                      │                                                                                                                                 
  ├───────────────────┼──────────────────────────────────────────────────────┤                                                                                                                                   
  │ KEYSTORE_BASE64   │ Base64-encoded keystore (base64 -i release.keystore) │
  ├───────────────────┼──────────────────────────────────────────────────────┤                                                                                                                                   
  │ KEYSTORE_PASSWORD │ Keystore password                                    │                                                                                                                                   
  ├───────────────────┼──────────────────────────────────────────────────────┤                                                                                                                                   
  │ KEY_ALIAS         │ Key alias                                            │                                                                                                                                   
  ├───────────────────┼──────────────────────────────────────────────────────┤                                                                                                                                   
  │ KEY_PASSWORD      │ Key password                                         │                                                                                                                                 
  └───────────────────┴──────────────────────────────────────────────────────┘

  Project Structure                                                                                                                                                                                              
   
  app/src/main/java/com/example/voidloltg_sms/                                                                                                                                                                   
  ├── MainActivity.kt            # Compose UI — settings, permissions, status                                                                                                                                  
  ├── SmsReceiver.kt             # BroadcastReceiver for incoming SMS                                                                                                                                            
  ├── BatteryAlertReceiver.kt    # BroadcastReceiver for low battery                                                                                                                                             
  ├── TelegramSender.kt          # HTTP client for Telegram Bot API                                                                                                                                              
  ├── AppSettings.kt             # Data class for bot token & chat ID                                                                                                                                            
  ├── AppSettingsStore.kt        # SharedPreferences persistence                                                                                                                                                 
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
                                                                                                                                                                                                               
  Permissions                                                                                                                                                                                                    
                                                                                                                                                                                                               
  ┌────────────────────┬────────────────────────────────────┐                                                                                                                                                    
  │     Permission     │              Purpose               │
  ├────────────────────┼────────────────────────────────────┤                                                                                                                                                    
  │ INTERNET           │ Send messages to Telegram Bot API  │                                                                                                                                                  
  ├────────────────────┼────────────────────────────────────┤
  │ RECEIVE_SMS        │ Intercept incoming SMS             │
  ├────────────────────┼────────────────────────────────────┤                                                                                                                                                    
  │ READ_PHONE_STATE   │ Identify the receiving SIM         │
  ├────────────────────┼────────────────────────────────────┤                                                                                                                                                    
  │ READ_PHONE_NUMBERS │ Read SIM phone number              │                                                                                                                                                  
  ├────────────────────┼────────────────────────────────────┤
  │ READ_CONTACTS      │ Resolve sender names from contacts │
  └────────────────────┴────────────────────────────────────┘                                                                                                                                                    
   
  Privacy                                                                                                                                                                                                        
                                                                                                                                                                                                               
  - No data leaves your device except the SMS content sent to your own Telegram bot                                                                                                                              
  - No analytics, no tracking, no third-party services
  - Bot token and chat ID are stored in local SharedPreferences only                                                                                                                                             
                                                                                                                                                                                                                 
  License                                                                                                                                                                                                        
                                                                                                                                                                                                                 
  MIT                                                                                                                                                                                                          
  ```
