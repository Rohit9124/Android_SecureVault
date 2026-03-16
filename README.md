🔐 SecureVault – Secure Password Manager for Android

SecureVault is a secure Android-based password manager designed to store, manage, and autofill credentials safely using encryption and advanced security features. The application focuses on privacy, security, and usability by keeping all sensitive data encrypted and stored locally on the device.

📱 Features :

🔑 Secure Password Storage
Stores credentials using AES encryption
All sensitive data is encrypted before being saved in the local database

⚡ Autofill Integration
Uses Android Autofill Framework
Automatically fills login credentials in supported apps and browsers

🔒 Emergency Lock (Security Feature)
Combines multiple protection mechanisms:
Lock when screen turns off
Lock when app goes to background
Panic trigger (shake phone twice)

🧠 Password Health & Security Score
Analyzes stored credentials and provides insights about:
Weak passwords
Password reuse
Old credentials

📋 Auto-Clear Clipboard
Copied passwords are automatically removed from clipboard after a selected time.

🌙 Dark Mode Support
Fully optimized UI for both light mode and dark mode.

🚫 Screenshot Protection
Screenshots and screen recording are blocked to prevent data leakage.

📤 Export & Import
Users can:
Export encrypted database
Import backup securely

🛠 Tech Stack
Technology	Purpose
Java	Core Android development
Android SDK	Application framework
SQLite	Local encrypted database
AES Encryption	Secure data storage
Android Autofill API	Credential autofill
Biometric API	Fingerprint authentication
Material UI	Modern Android UI components


🏗 Project Architecture
The project follows a modular Android architecture:

SecureVault
│
├── activities
│   ├── LoginActivity
│   ├── MainViewActivity
│
├── fragments
│   ├── AddPasswordFragment
│   ├── UpdatePasswordFragment
│   ├── SettingsFragment
│   ├── SecureNotesFragment
│   ├── SecureAttachmentsFragment
│
├── database
│   ├── DatabaseHelper
│
├── encryption
│   ├── EncryptionHelper
│
├── autofill
│   ├── SecureVaultAutofillService
│   ├── AutofillFieldsParser
│
├── utilities
│   ├── AppLockManager
│   ├── DialogHelper
│   ├── SettingsPrefs
│
└── worker
    ├── ClipboardClearWorker
    
📦 Installation
1️⃣ Clone the Repository
git clone https://github.com/yourusername/SecureVault.git
2️⃣ Open in Android Studio
Open the project folder in Android Studio.
3️⃣ Build the Project
Build → Make Project
4️⃣ Run on Device

Connect your Android device and click Run.
📲 APK Installation
If using the APK directly:
Download the APK
Enable Install from Unknown Sources
Install the APK on your Android device

🔐 Security Features
SecureVault implements several security mechanisms:
AES encryption for stored data
Biometric authentication
Emergency lock system
Screenshot blocking
Clipboard auto-clear
Secure local storage
Autofill credential protection

🧪 Testing
Testing performed includes:
Unit Testing
Integration Testing
Beta Testing on real Android devices
Autofill compatibility testing with apps and browsers

🚀 Future Improvements
Possible enhancements include:
Cloud backup support
Multi-device synchronization
Password breach detection
Two-factor authentication
Secure password sharing
Browser extension integration

📚 References
Android Developer Documentation
OWASP Mobile Security Guidelines
Android Autofill Framework Documentation
Cryptography Best Practices

👨‍💻 Author
Rohit
Android Developer
Project: SecureVault – Secure Password Manager

⭐ License
This project is intended for educational and academic purposes.
