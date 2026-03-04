<p align="center">
  <img src="https://img.shields.io/badge/TABAANI-User%20Management-blueviolet?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0id2hpdGUiPjxwYXRoIGQ9Ik0xMiAyQzYuNDggMiAyIDYuNDggMiAxMnM0LjQ4IDEwIDEwIDEwIDEwLTQuNDggMTAtMTBTMTcuNTIgMiAxMiAyem0wIDNjMS42NiAwIDMgMS4zNCAzIDNzLTEuMzQgMy0zIDMtMy0xLjM0LTMtMyAxLjM0LTMgMy0zem0wIDE0LjJjLTIuNSAwLTQuNzEtMS4yOC02LTMuMjIuMDMtMS45OSA0LTMuMDggNi0zLjA4IDEuOTkgMCA1Ljk3IDEuMDkgNiAzLjA4LTEuMjkgMS45NC0zLjUgMy4yMi02IDMuMjJ6Ii8+PC9zdmc+" alt="TABAANI"/>
</p>

<h1 align="center">🏢 TABAANI — User Management System</h1>

<p align="center">
  <em>A secure, feature-rich desktop application for user management built with JavaFX</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 17"/>
  <img src="https://img.shields.io/badge/JavaFX-22-0078D4?style=flat-square&logo=java&logoColor=white" alt="JavaFX 22"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven"/>
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="License"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Face%20Recognition-OpenCV-5C3EE8?style=flat-square&logo=opencv&logoColor=white" alt="OpenCV"/>
  <img src="https://img.shields.io/badge/2FA-TOTP%20%7C%20SMS%20%7C%20Email-FF6F00?style=flat-square&logo=authelia&logoColor=white" alt="2FA"/>
  <img src="https://img.shields.io/badge/SMS-Twilio-F22F46?style=flat-square&logo=twilio&logoColor=white" alt="Twilio"/>
</p>

---

## ✨ Features at a Glance

| Category | Features |
|:---------|:---------|
| 🔐 **Authentication** | Email/Password login, Face Recognition login, Forgot password with OTP |
| 🛡️ **Two-Factor Auth** | TOTP (Google Authenticator), SMS OTP (Twilio), Email OTP — all 3 methods |
| 👤 **User Management** | Full CRUD, role-based access, profile pictures, audit logging |
| 📊 **Reporting** | Export to PDF (with charts & stats), Excel, CSV |
| 🎨 **Theming** | Dark/Light mode toggle with persistent preferences |
| ⏱️ **Session Security** | Auto-timeout with warning popup after inactivity |
| 🧑‍💼 **Admin Dashboard** | User statistics, role management, permission control |

---

## 🏗️ Architecture

```
src/main/java/com/esprit/
├── Main.java                          # Application entry point
├── controllers/
│   ├── LoginController.java           # Auth + Face Recognition + 2FA
│   ├── signupController.java          # Registration with OTP verification
│   ├── ForgotPasswordController.java  # Password recovery flow
│   ├── MainInterfaceController.java   # User dashboard
│   ├── AdminController.java           # Admin panel (CRUD, export, audit)
│   ├── DashboardController.java       # Statistics & charts dashboard
│   └── UserProfileController.java     # Profile, security, 2FA settings
├── entities/
│   ├── utilisateur.java               # User entity
│   ├── role.java                      # Role entity
│   ├── RolePermission.java            # Permission mapping
│   └── AuditLog.java                  # Audit trail entity
├── services/
│   ├── utilisateurServices.java       # User CRUD operations
│   ├── roleServices.java              # Role management
│   ├── TOTPService.java               # TOTP 2FA (Google Authenticator)
│   ├── SmsOTPService.java             # SMS OTP via Twilio
│   ├── OTPService.java                # Email OTP verification
│   ├── EmailService.java              # SMTP email service
│   ├── FaceRecognitionService.java    # OpenCV face detection & matching
│   ├── ExportService.java             # CSV / Excel / PDF export
│   ├── ImportService.java             # CSV / Excel import
│   ├── AuditLogService.java           # Audit trail logging
│   └── RolePermissionService.java     # Permission management
└── utils/
    ├── MyDataBase.java                # MySQL singleton connection
    ├── ThemeManager.java              # Dark/Light theme persistence
    └── SessionManager.java            # Inactivity timeout manager
```

---

## 🔐 Security Deep Dive

### Multi-Layer Authentication

```
┌─────────────────────────────────────────────────────────┐
│                    LOGIN FLOW                            │
├─────────────────────────────────────────────────────────┤
│                                                         │
│   ┌──────────┐    ┌──────────────┐    ┌─────────────┐  │
│   │ Password │ OR │    Face      │ OR │   Forgot    │  │
│   │  Login   │    │ Recognition  │    │  Password   │  │
│   └────┬─────┘    └──────┬───────┘    └──────┬──────┘  │
│        │                 │                    │         │
│        └────────┬────────┘                    │         │
│                 ▼                              │         │
│        ┌────────────────┐              ┌──────▼──────┐  │
│        │  2FA Enabled?  │              │  OTP Email  │  │
│        └───┬────────┬───┘              │  Verify →   │  │
│         No │     Yes│                  │  New Pass   │  │
│            ▼        ▼                  └─────────────┘  │
│     ┌──────────┐  ┌─────────────────┐                   │
│     │  Access  │  │  2FA Challenge  │                   │
│     │ Granted  │  │  ┌───────────┐  │                   │
│     └──────────┘  │  │   TOTP    │  │                   │
│                   │  ├───────────┤  │                   │
│                   │  │ SMS OTP   │  │                   │
│                   │  ├───────────┤  │                   │
│                   │  │ Email OTP │  │                   │
│                   │  └───────────┘  │                   │
│                   └────────┬────────┘                   │
│                            ▼                            │
│                   ┌────────────────┐                    │
│                   │ Access Granted │                    │
│                   └────────────────┘                    │
└─────────────────────────────────────────────────────────┘
```

### Face Recognition Login
- Powered by **OpenCV** (JavaCV) with Haar Cascade classifiers
- Real-time webcam capture with face detection overlay
- Face encoding stored securely in database
- Confidence threshold matching for verification

### Two-Factor Authentication (2FA)
- **TOTP** — Time-based One-Time Password (RFC 6238) with QR code setup
- **SMS OTP** — Delivered via Twilio API
- **Email OTP** — Fallback via SMTP
- Enable/disable directly from User Profile security settings

---

## 🎨 Theming

| Dark Mode 🌙 | Light Mode ☀️ |
|:---:|:---:|
| Default modern dark theme | Clean light alternative |

- Toggle from User Profile → Settings
- Preference persisted across sessions using `java.util.prefs`
- Applied globally across all views instantly

---

## 📊 Export & Reporting

Generate professional reports with one click:

- **PDF Reports** — Includes:
  - 📈 User statistics cards (total, active, blocked, admins)
  - 🔒 Security overview (2FA adoption, face recognition usage)
  - 📊 Role distribution chart (JFreeChart embedded)
  - 📋 Full user table with formatted data
- **Excel (.xlsx)** — Apache POI powered spreadsheets
- **CSV** — Lightweight data export via OpenCSV

---

## 🛠️ Tech Stack

| Layer | Technology |
|:------|:-----------|
| **Language** | Java 17 |
| **UI Framework** | JavaFX 22 + FXML + CSS |
| **Database** | MySQL 8.0 (JDBC) |
| **Build Tool** | Apache Maven |
| **Face Recognition** | OpenCV via JavaCV 1.5.10 |
| **2FA / TOTP** | Google Authenticator (warrenstrange/googleauth) |
| **QR Codes** | ZXing 3.5.3 |
| **SMS** | Twilio SDK 10.1.5 |
| **Email** | JavaMail 1.6.2 (SMTP) |
| **PDF Generation** | OpenPDF + iText 7 + JFreeChart |
| **Excel I/O** | Apache POI 5.2.5 |
| **CSV I/O** | OpenCSV 5.9 |
| **Password Strength** | nbvcxz (zxcvbn Java port) |
| **Testing** | JUnit 5 |

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+** (JDK)
- **Maven 3.8+**
- **MySQL 8.0+**
- A webcam (for face recognition features)

### Database Setup

```sql
CREATE DATABASE tabaani_db;
```

> Configure your connection in `src/main/java/com/esprit/utils/MyDataBase.java`

### Build & Run

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/tabaani-user-management.git
cd tabaani-user-management

# Build the project
mvn clean install

# Run the application
mvn javafx:run
```

### Twilio SMS Setup (Optional)

To enable SMS OTP, configure your Twilio credentials in `SmsOTPService.java`:
```java
private static final String ACCOUNT_SID = "your_account_sid";
private static final String AUTH_TOKEN = "your_auth_token";
private static final String FROM_PHONE = "your_twilio_number";
```

---

## 📁 Project Structure

```
G_User/
├── pom.xml                    # Maven configuration & dependencies
├── README.md                  # You are here 👋
└── src/
    ├── main/
    │   ├── java/com/esprit/   # Application source code
    │   └── resources/
    │       ├── *.fxml         # UI layouts (7 views)
    │       ├── style.css      # Dark theme (default)
    │       ├── style-light.css# Light theme
    │       └── haarcascade_frontalface_alt.xml  # Face detection model
    └── test/
        └── java/              # Unit tests
```

---

## 👥 Role-Based Access

| Role | Access Level |
|:-----|:-------------|
| **Admin** | Full access — Admin dashboard, user CRUD, export/import, audit logs, role management |
| **User** | Personal dashboard, profile management, 2FA settings, ticket submission |

After login, users are automatically redirected to their role-appropriate interface.

---

## ⏱️ Session Management

- **Auto-timeout**: 10 minutes of inactivity
- **Warning popup**: Appears 60 seconds before logout
- **Graceful redirect**: Returns to login screen on timeout
- Activity tracking: mouse movement, keyboard input, clicks

---

## 🔮 Roadmap

- [ ] OAuth 2.0 (Google / GitHub login)
- [ ] Real-time notifications (WebSocket)
- [ ] Biometric fingerprint support
- [ ] Multi-language i18n support
- [ ] Activity analytics dashboard
- [ ] Password encryption (bcrypt)

---

## 📝 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions are welcome! Feel free to:

1. Fork the project
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

<p align="center">
  <b>Built with ❤️ by the TABAANI Team</b>
  <br/>
  <sub>Esprit School of Engineering — 3A17</sub>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/⭐_Star_this_repo-If_you_found_it_useful!-yellow?style=for-the-badge" alt="Star"/>
</p>
