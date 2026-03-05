<p align="center">
  <img src="https://img.shields.io/badge/TABAANI-SmartTravel-blueviolet?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0id2hpdGUiPjxwYXRoIGQ9Ik0xMiAyQzYuNDggMiAyIDYuNDggMiAxMnM0LjQ4IDEwIDEwIDEwIDEwLTQuNDggMTAtMTBTMTcuNTIgMiAxMiAyem0wIDNjMS42NiAwIDMgMS4zNCAzIDNzLTEuMzQgMy0zIDMtMy0xLjM0LTMtMyAxLjM0LTMgMy0zem0wIDE0LjJjLTIuNSAwLTQuNzEtMS4yOC02LTMuMjIuMDMtMS45OSA0LTMuMDggNi0zLjA4IDEuOTkgMCA1Ljk3IDEuMDkgNiAzLjA4LTEuMjkgMS45NC0zLjUgMy4yMi02IDMuMjJ6Ii8+PC9zdmc+" alt="TABAANI"/>
</p>

<h1 align="center">🌍 TABAANI — SmartTravel Tourism Platform</h1>

<p align="center">
  <em>Developed at <strong>Esprit School of Engineering – Tunisia</strong></em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 17"/>
  <img src="https://img.shields.io/badge/JavaFX-22-0078D4?style=flat-square&logo=java&logoColor=white" alt="JavaFX 22"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven"/>
  <img src="https://img.shields.io/badge/Gemini%20AI-Chatbot-4285F4?style=flat-square&logo=google&logoColor=white" alt="Gemini AI"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Face%20Recognition-OpenCV-5C3EE8?style=flat-square&logo=opencv&logoColor=white" alt="OpenCV"/>
  <img src="https://img.shields.io/badge/Payment-Paymee-FF6F00?style=flat-square&logo=paypal&logoColor=white" alt="Paymee"/>
  <img src="https://img.shields.io/badge/2FA-TOTP%20%7C%20SMS%20%7C%20Email-FF6F00?style=flat-square&logo=authelia&logoColor=white" alt="2FA"/>
  <img src="https://img.shields.io/badge/QR%20Code-ZXing-000000?style=flat-square" alt="QR Code"/>
</p>

---

## 📖 Overview

**TABAANI SmartTravel** is a comprehensive desktop tourism management platform developed as an academic project at **Esprit School of Engineering**, Tunisia (Academic Year 2024–2025). The application provides a full-featured ecosystem for tourists, business partners (partenaires), and administrators to manage tourist destinations, accommodations, reservations, and payments across Tunisia.

Built with JavaFX and powered by Gemini AI, the platform features role-based access control for three distinct user types — **Tourist**, **Partenaire** (business partner), and **Administrator** — each with a dedicated interface and tailored functionalities.

---

## ✨ Features

### 🔐 Authentication & Security
| Feature | Description |
|:--------|:------------|
| Multi-method Login | Email/password, Face Recognition (OpenCV), Google Authenticator TOTP |
| Two-Factor Authentication | TOTP, SMS OTP (Twilio), Email OTP — three independent methods |
| Forgot Password | Secure OTP-based password recovery via email |
| Session Security | Auto-timeout with warning popup after inactivity |
| Audit Logging | Complete admin audit trail for all system actions |

### 🌍 Tourist Features
| Feature | Description |
|:--------|:------------|
| Browse Destinations | Explore lieux touristiques and establishments with carousel UI |
| Smart Cart (Panier) | Add destinations to cart and manage bookings |
| Online Payment | Integrated Paymee payment gateway (sandbox) |
| Cash Payment | Pay in cash with partenaire approval flow |
| QR Code Confirmation | Reservation confirmation via QR code generation |
| AI Chatbot | Gemini AI-powered tourism assistant with multi-model fallback |
| Forum | Community discussion forum for travelers |
| Multi-language | Global language toggle with real-time translation |

### 🤝 Partenaire (Business Partner) Features
| Feature | Description |
|:--------|:------------|
| Dedicated Dashboard | Full partenaire interface with statistics |
| Establishment Management | CRUD for hotels, restaurants, and cafes |
| Lieu Touristique Management | Manage affiliated tourist destinations |
| Cash Reservation Approval | Approve or reject cash payment reservations |
| Category & Address Management | Manage categories and addresses |
| Forum Access | Community engagement through the forum |

### 🛡️ Administrator Features
| Feature | Description |
|:--------|:------------|
| Admin Dashboard | Comprehensive analytics and statistics |
| User Management | Full CRUD, role control, block/unblock users |
| Partenaire Approval | Review and approve partenaire registration requests |
| Restaurant Reservation Management | Confirm or reject restaurant reservations |
| Permission Control | Granular access management |
| Reports & Export | Export to PDF (with charts), Excel, CSV |
| Dark/Light Theme | System-wide theme toggle with persistent preferences |

---

## 🛠️ Tech Stack

| Layer | Technology |
|:------|:-----------|
| **Language** | Java 17 |
| **UI Framework** | JavaFX 22 |
| **Build Tool** | Apache Maven |
| **Database** | MySQL 8.0 (via XAMPP) |
| **AI** | Google Gemini API (multi-model fallback) |
| **Payment** | Paymee Payment Gateway |
| **Face Recognition** | OpenCV (via JavaCV) with Haar Cascade |
| **QR Codes** | ZXing library |
| **2FA** | Google Authenticator TOTP, Twilio SMS, Email OTP |
| **Email** | JavaMail (Gmail SMTP) |
| **Translation** | Gemini AI-powered real-time translation |
| **Styling** | Custom CSS (Dark/Light themes) |

---

## 🏗️ Architecture

```
src/main/java/com/esprit/
├── Main.java                              # Application entry point
├── controllers/
│   ├── LoginController.java               # Authentication (Login + Face + 2FA)
│   ├── signupController.java              # Registration (Tourist + Partenaire)
│   ├── ForgotPasswordController.java      # Password recovery
│   ├── MainInterfaceController.java       # Tourist dashboard
│   ├── PartenaireInterfaceController.java # Partenaire dashboard
│   ├── DashboardController.java           # Admin dashboard
│   ├── UserProfileController.java         # User profile management
│   ├── PanierController.java              # Shopping cart
│   ├── ReservationController.java         # Reservation & payment
│   └── forum/                             # Forum controllers
├── entities/
│   ├── utilisateur.java                   # User entity
│   ├── LieuTouristique.java              # Tourist destination entity
│   ├── Etablissement.java                 # Establishment entity
│   ├── Reservation.java                   # Reservation entity
│   ├── Panier.java                        # Cart entity
│   └── ...                                # Categories, Addresses, etc.
├── services/
│   ├── utilisateurServices.java           # User CRUD + role management
│   ├── LieuTouristiqueServices.java       # Destination services
│   ├── EtablissementService.java          # Establishment services
│   ├── ReservationService.java            # Reservation services
│   ├── GeminiService.java                 # Gemini AI integration
│   ├── EmailService.java                  # Email notifications
│   ├── TranslationService.java            # Multi-language support
│   └── ...                                # Panier, Audit, TOTP, etc.
└── utils/
    ├── MyDataBase.java                    # Database connection
    ├── ThemeManager.java                  # Dark/Light themes
    ├── SessionManager.java                # Session handling
    └── ...                                # Helpers and utilities
```

```
src/main/resources/
├── *.fxml                # UI layout files (25+ screens)
├── *.css                 # Theme stylesheets
├── config.properties     # Database configuration
├── gemini.properties     # AI API configuration
└── forum/                # Forum FXML files
```

---

## 👥 Contributors

<table>
  <tr>
    <td align="center"><b>Rayen Berkaoui</b><br/><sub>Project Lead</sub></td>
  </tr>
</table>

> **Class:** 3A31 — Esprit School of Engineering, Tunisia  
> **Academic Year:** 2024–2025  
> **Project Type:** PI-DEV (Projet Intégré de Développement)

---

## 🎓 Academic Context

This project was developed as part of the **PI-DEV** (Projet Intégré de Développement) curriculum at **Esprit School of Engineering**, a leading engineering institution in Tunisia. The project integrates multiple software engineering disciplines including:

- Full-stack desktop application development (JavaFX)
- Database design and management (MySQL)
- AI integration (Google Gemini)
- Payment gateway integration (Paymee)
- Computer vision (Face recognition with OpenCV)
- Security best practices (2FA, audit logging, role-based access)
- UI/UX design (Dark/Light themes, responsive layouts)

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** (JDK)
- **Apache Maven** 3.9+
- **MySQL** (via XAMPP or standalone)
- **Git**

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/rayen-berkaoui/Java_Project.git
   cd Java_Project
   ```

2. **Configure the database:**
   - Start XAMPP (MySQL)
   - Create database `tabaany`
   - Import the SQL schema: `tabaany.sql`
   - Update credentials in `src/main/java/com/esprit/utils/MyDataBase.java`

3. **Configure API keys:**
   - Gemini API key in `src/main/resources/gemini.properties`
   - Paymee token in the reservation service

4. **Build and run:**
   ```bash
   mvn clean compile
   mvn javafx:run
   ```

### Default Roles
| Role | ID | Access |
|:-----|:---|:-------|
| Tourist | 1 | Browse, Cart, Payments, Forum |
| Partenaire | 2 | Establishment management, Cash approval |
| Admin | 3 | Full system administration |

---

## 🙏 Acknowledgments

- **Esprit School of Engineering** — For providing the academic framework and guidance
- **Google Gemini AI** — Powering the AI chatbot and translation services
- **Paymee** — Payment gateway integration for the Tunisian market
- **OpenCV / JavaCV** — Face recognition capabilities
- **ZXing** — QR code generation library

---

<p align="center">
  <b>Built with ❤️ by the TABAANI Team</b>
  <br/>
  <sub>Developed at <strong>Esprit School of Engineering</strong> — Tunisia | Class 3A31 | 2024–2025</sub>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/⭐_Star_this_repo-If_you_found_it_useful!-yellow?style=for-the-badge" alt="Star"/>
</p>

2. **Configure the database:**
   - Start XAMPP (MySQL)
   - Create database `tabaany`
   - Import the SQL schema: `tabaany.sql`
   - Update credentials in `src/main/java/com/esprit/utils/MyDataBase.java`

3. **Configure API keys:**
   - Gemini API key in `src/main/resources/gemini.properties`
   - Paymee token in the reservation service

4. **Build and run:**
   ```bash
   mvn clean compile
   mvn javafx:run
   ```

### Default Roles
| Role | ID | Access |
|:-----|:---|:-------|
| Tourist | 1 | Browse, Cart, Payments, Forum |
| Partenaire | 2 | Establishment management, Cash approval |
| Admin | 3 | Full system administration |

---

## 🙏 Acknowledgments

- **Esprit School of Engineering** — For providing the academic framework and guidance
- **Google Gemini AI** — Powering the AI chatbot and translation services
- **Paymee** — Payment gateway integration for the Tunisian market
- **OpenCV / JavaCV** — Face recognition capabilities
- **ZXing** — QR code generation library

---

<p align="center">
  <b>Built with ❤️ by the TABAANI Team</b>
  <br/>
  <sub>Developed at <strong>Esprit School of Engineering</strong> — Tunisia | Class 3A31 | 2024–2025</sub>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/⭐_Star_this_repo-If_you_found_it_useful!-yellow?style=for-the-badge" alt="Star"/>
</p>
