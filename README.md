# QuickInvoice Mobile 📱

An Android mobile application for **QuickInvoice** — a full-stack invoice generation platform. Built with Kotlin and Android Studio, this app connects to the existing QuickInvoice Spring Boot backend.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Architecture | MVVM |
| HTTP Client | Retrofit 2 + OkHttp |
| Auth | Clerk (JWT-based) |
| Database | MongoDB Atlas (via backend) |
| Backend | Spring Boot (REST API) |
| Min SDK | API 26 (Android 8.0 Oreo) |

---

## 📁 Project Structure

```
com.sarsonasgino.quickinvoicemobile
├── model/
│   ├── Invoice.kt
│   ├── User.kt
│   └── AuthModels.kt
├── network/
│   ├── ApiService.kt
│   ├── RetrofitClient.kt
│   ├── ClerkApiService.kt
│   └── ClerkRetrofitClient.kt
├── repository/
├── viewmodel/
├── ui/
│   ├── LandingActivity.kt
│   ├── LoginActivity.kt
│   └── RegisterActivity.kt
└── MainActivity.kt
```

---

## 🔌 API Endpoints Used

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/users` | Create or update user profile |
| `GET` | `/api/invoices` | Fetch all invoices for logged-in user |
| `POST` | `/api/invoices` | Create a new invoice |
| `DELETE` | `/api/invoices/{id}` | Delete an invoice |
| `POST` | `/api/invoices/sendinvoice` | Send invoice PDF via email |
| `GET` | `/api/subscription/status` | Get subscription status |
| `POST` | `/api/subscription/upgrade` | Upgrade to Premium |
| `POST` | `/api/subscription/cancel` | Cancel subscription |

---

## 🔐 Authentication

Authentication is handled via **Clerk**. The app communicates with the Clerk frontend API at:

```
https://hardy-gar-20.clerk.accounts.dev/
```

On successful login, a **JWT token** is extracted from the Clerk session and attached as a `Bearer` token to all backend API requests via an OkHttp interceptor.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Iguana (2023.2.1) or higher
- JDK 8+
- A running instance of the [QuickInvoice Backend](https://github.com/your-username/invoice-backend)

### Setup

1. Clone the repository
```bash
git clone https://github.com/your-username/quickinvoice-mobile.git
```

2. Open in Android Studio

3. Let Gradle sync complete

4. Run your Spring Boot backend locally on port `8080`

5. Run the app on an emulator or physical device

> **Note:** The emulator uses `10.0.2.2:8080` to reach your local machine's `localhost:8080`. For a physical device on the same WiFi, use your PC's local IP address.

---

## 📱 Features

- [x] User authentication (Login & Register via Clerk)
- [x] View all invoices
- [x] Create new invoices
- [x] Delete invoices
- [ ] Send invoice via email
- [ ] Premium subscription (Stripe)
- [ ] Invoice templates (Free & Premium)

---

## 🔗 Related Repositories

- [QuickInvoice Backend](https://github.com/your-username/invoice-backend) — Spring Boot + MongoDB Atlas
- [QuickInvoice Frontend](https://github.com/your-username/invoice-frontend) — React + Vite + Clerk + Stripe

---

## 👨‍💻 Author

**Kervin Gino M. Sarsonas**  
BSIT Student — Cebu Institute of Technology University  
Team 0xACE

---

## 📄 License

This project is for academic and personal use only.
