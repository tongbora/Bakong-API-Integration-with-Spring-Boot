# Bakong API Integration with Spring Boot

A **Spring Boot** project demonstrating how to integrate with the **Bakong Open API** and **KHQR SDK** — Cambodia's official QR payment system developed by the **National Bank of Cambodia (NBC)**. This project covers generating **KHQR** payment QR codes, rendering scannable QR images, and verifying transactions using the **Bakong Java SDK**.

> 🇰🇭 If you are a developer in Cambodia looking to integrate **Bakong QR payment**, **KHQR**, or the **NBC Open API** into your Java or Spring Boot application — this project is for you.

---

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Resources & Documentation](#resources--documentation)
- [Project Setup](#project-setup)
- [Dependencies](#dependencies)
- [Configuration](#configuration)
- [Token Auto-Renewal](#token-auto-renewal)
- [API Endpoints](#api-endpoints)
- [Usage Guide](#usage-guide)
- [KHQR Card UI Design](#khqr-card-ui-design)
- [KHQR Assets](#khqr-assets)
- [Project Structure](#project-structure)
- [Notes](#notes)

---

## Prerequisites

Before starting the integration, you need to:

1. **Create a Bakong Account** — Register at [https://bakong.nbc.gov.kh](https://bakong.nbc.gov.kh) and get your account verified.
2. **Register with Bakong Open API** — Sign up with your email at [https://api-bakong.nbc.gov.kh/](https://api-bakong.nbc.gov.kh/) to receive your **Bearer Token** for API access.

---

## Resources & Documentation

Read these documents before starting the integration. All PDFs are also available locally inside the `integration/` folder of this project.

| Resource | Description | Link |
|---|---|---|
| Bakong Open API Document | Core API reference for authentication and endpoints | [Download PDF](https://bakong.nbc.gov.kh/download/KHQR/integration/Bakong%20Open%20API%20Document.pdf) |
| KHQR SDK Document | Guide for using the KHQR Java SDK | [Download PDF](https://bakong.nbc.gov.kh/download/KHQR%20SDK.pdf) |
| KHQR Content Guideline v1.3 | Content and data field standards for KHQR | `integration/KHQR Content Guideline v.1.3.pdf` |
| QR Payment Integration | End-to-end QR payment integration guide | `integration/QR Payment Integration.pdf` |
| KHQR Card Guideline | Official UI/design guideline for KHQR card display | [Download PDF](https://bakong.nbc.gov.kh/en/download/KHQR/guideline/KHQR%20Card%20Guideline.pdf) |
| Bakong Open API Portal | Register and get your Bearer Token | [https://api-bakong.nbc.gov.kh/](https://api-bakong.nbc.gov.kh/) |

---

## Project Setup

Clone or create a Spring Boot project and add the required dependencies listed below.

---

## Dependencies

Add the following to your `build.gradle`:

```groovy
// Bakong KHQR SDK
implementation 'kh.gov.nbc.bakong_khqr:sdk-java:1.0.0.11'

// QR code image generation (ZXing)
implementation 'com.google.zxing:core:3.5.3'
implementation 'com.google.zxing:javase:3.5.3'

// Dynamic JSON parsing
implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
```

---

## Configuration

### `application.properties`

```properties
spring.application.name=bakong-api-integration
# Bakong Configuration (loaded from environment or profile properties)
bakong.account-id=${BAKONG_ACCOUNT_ID}
bakong.acquiring-bank=${ACQUIRINGBANK}
bakong.merchant-name=${MERCHANTNAME}
bakong.mobile-number=${MOBILENUMBERSTORELABEL}
bakong.store-label=${STORELABEL}
bakong.base-url=${BAKONG_BASE_URL}
bakong.email=${EMAIL}
```

### `application-dev.properties`

```properties
BAKONG_ACCOUNT_ID=your_bakong_id@bank
ACQUIRINGBANK=your_acquiring_bank_code
MERCHANTNAME=Your-Merchant-Name
MOBILENUMBERSTORELABEL=YourLabel
STORELABEL=YourStoreLabel
BAKONG_BASE_URL=https://api-bakong.nbc.gov.kh
EMAIL=your_registered_email@example.com
```

> ⚠️ **Security Warning:** Never commit real credentials to version control. Use environment variables or a secrets manager in production.

> 💡 **No need to manage tokens manually.** The `BakongTokenService` automatically fetches and caches the Bearer Token using your registered email. It decodes the JWT expiry and renews the token automatically before it expires.

---

## Token Auto-Renewal

Previously, you had to manually copy and paste the Bearer Token from the Bakong portal into your config. This project handles token management automatically.

The `BakongTokenService` works as follows:

1. On the first request, it calls the Bakong `/v1/renew_token` endpoint using your registered **email address**
2. It decodes the returned **JWT** to read the real expiry time (`exp` claim)
3. The token is **cached in memory** — subsequent requests reuse it without hitting the API again
4. When the token is **expired**, it automatically fetches a new one

This means you only need to provide your `EMAIL` in the config — no manual token copying needed.

```
POST /v1/renew_token
Body: { "email": "your_registered_email@example.com" }
```

> 🔒 The token is stored in memory only (not on disk). It is renewed per application instance and resets on restart.

---

## API Endpoints

All endpoints are prefixed with `/api/v1/bakong`.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/generate-qr` | Generates a KHQR string and MD5 hash |
| `POST` | `/get-qr-image` | Converts a KHQR string into a scannable PNG image |
| `POST` | `/check-transaction` | Checks if a transaction has been completed using its MD5 hash |

---

## Usage Guide

Follow these steps in order to complete a payment flow:

### Step 1 — Generate QR Code

Call the `generate-qr` endpoint with the payment amount. This returns a KHQR string and an MD5 hash.

**Request:**

```
POST http://localhost:8080/api/v1/bakong/generate-qr
Content-Type: application/json

{
    "amount": 0.1
}
```

**Response:**

```json
{
    "KHQRStatus": {
        "code": 0,
        "errorCode": null,
        "message": null
    },
    "data": {
        "md5": "2e8787edaddc31ffe9c572923db06d33",
        "qr": "0002010102121511KH12345678930360014bora_tong@aclb0106..."
    }
}
```

Save both the `qr` string and the `md5` value — you will need them in the next steps.

---

### Step 2 — Get QR Image

Pass the `qr` string from Step 1 to generate a scannable PNG QR code image.

**Request:**

```
POST http://localhost:8080/api/v1/bakong/get-qr-image
Content-Type: application/json

{
    "qr": "0002010102121511KH12345678930360014bora_tong@aclb0106...",
    "md5": "2e8787edaddc31ffe9c572923db06d33"
}
```

**Response:**

The endpoint returns a PNG image (`image/png`). Display or download the image so the customer can scan it using the Bakong app to complete the payment.

---

### Step 3 — Check Transaction Status

After the customer scans and pays, use the `md5` hash from Step 1 to verify whether the payment was successful.

The response is mapped to a `BakongResponse` record:

```java
public record BakongResponse(
    int responseCode,
    String responseMessage,
    Integer errorCode,
    Object data
)
```

> ✅ **`responseCode: 0`** — Transaction successful
> ❌ **`responseCode: 1`** — Transaction not found or not yet paid

**Request:**

```
POST http://localhost:8080/api/v1/bakong/check-transaction
Content-Type: application/json

{
    "md5": "2e8787edaddc31ffe9c572923db06d33"
}
```

**Response — Payment Successful (`responseCode: 0`):**

```json
{
    "responseCode": 0,
    "responseMessage": "Success",
    "data": {
        "hash": "bf917e9534cac3595ee5dc5a9e7d3b120b6143ff3b368c244189cf22ed9af877",
        "fromAccountId": "customer@bank",
        "toAccountId": "bora_tong@aclb",
        "currency": "USD",
        "amount": 0.1,
        "description": null,
        "createdDateMs": 1772125349000,
        "acknowledgedDateMs": 1772125351000,
        "externalRef": "100FT36931627892"
    }
}
```

**Response — Payment Not Found (`responseCode: 1`):**

```json
{
    "responseCode": 1,
    "responseMessage": "Transaction could not be found. Please check and try again.",
    "errorCode": 1,
    "data": null
}
```

> 💡 You can check `responseCode` in your frontend or backend logic to determine the payment status:
> - `responseCode == 0` → Payment confirmed, proceed with order
> - `responseCode == 1` → Payment not yet made or MD5 is incorrect

---

## KHQR Card UI Design

The **KHQR Card** is a standardized payment card UI that displays the KHQR logo, merchant name, payment amount, and the scannable QR code. This is what a customer sees when they are about to make a payment.

> 🎨 **You are responsible for designing and building this UI yourself.** The National Bank of Cambodia (NBC) provides strict branding guidelines that must be followed when displaying KHQR in any application.

### Design Guidelines

Download and follow the official **KHQR Card Guideline** from NBC:

📄 [KHQR Card Guideline PDF](https://bakong.nbc.gov.kh/en/download/KHQR/guideline/KHQR%20Card%20Guideline.pdf)

The guideline covers:

- Correct usage of the KHQR logo (color, size, placement)
- Card layout, proportions, and color palette
- Typography and font rules
- Dos and Don'ts for displaying the QR code
- Safe zones and spacing requirements

### Example Card Layout

![KHQR Card Example](KHQR%20-%20asset/KHQR%20-%20digital%20payment.svg)

### How to Build It

1. Call `/generate-qr` to get the `qr` string and `md5`.
2. Call `/get-qr-image` with the `qr` string to get the PNG image.
3. Display the image inside your custom-designed KHQR card UI.
4. Use the official logos and assets from the `KHQR - asset/` folder (see [KHQR Assets](#khqr-assets) below).
5. Follow the card guideline PDF strictly for correct branding.

---

## KHQR Assets

Official KHQR brand assets are included in the `KHQR - asset/` folder of this project. These assets are provided by the National Bank of Cambodia and **must be used as-is** according to the KHQR branding guidelines.

| File | Format | Usage |
|---|---|---|
| `KHQR Logo red.png` / `.svg` / `.jpg` | PNG, SVG, JPG | Primary KHQR logo on red background — use in card header |
| `KHQR Logo.png` / `.svg` / `.jpg` | PNG, SVG, JPG | KHQR logo on white/transparent background |
| `KHQR available here - logo with bg.png` / `.svg` / `.jpg` | PNG, SVG, JPG | "KHQR Available Here" badge — use at point of sale |
| `KHQR - digital payment.svg` | SVG | Digital payment banner graphic |
| `QR Stand for export.svg` | SVG | QR stand display for physical/print usage |
| `QR Tag.svg` | SVG | QR tag for labeling or printing |

> 💡 **Tip:** Use the `.svg` versions whenever possible for sharp rendering at any screen size. Use `.png` for environments that do not support SVG.

> ⚠️ Do not modify, recolor, or distort any of the official KHQR assets. Refer to the [KHQR Card Guideline](https://bakong.nbc.gov.kh/en/download/KHQR/guideline/KHQR%20Card%20Guideline.pdf) for approved usage rules.

---

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com.tongbora.bakongapiintergration/
│   │       ├── config/
│   │       │   ├── JacksonConfig.java           # Jackson ObjectMapper
│   │       │   └── RestTemplateConfig.java      # RestTemplate bean
│   │       ├── controller/
│   │       │   └── BakongController.java        # REST API endpoints
│   │       ├── dto/
│   │       │   ├── BakongRequest.java           # Request DTO (amount)
│   │       │   ├── BakongResponse.java          # Response DTO
│   │       │   └── CheckTransactionRequest.java # Check transaction DTO
│   │       ├── service/
│   │       │   ├── BakongService.java           # Service interface
│   │       │   ├── BakongTokenService.java      # Token service interface
│   │       │   └── impl/
│   │       │       ├── BakongServiceImpl.java   # Business logic
│   │       │       └── BakongTokenServiceImpl.java # Token auto-renewal
│   │       └── BakongApiIntergrationApplication.java
│   └── resources/
│       ├── static/
│       ├── templates/
│       ├── application.properties
│       └── application-dev.properties
└── test/
```

> 📁 The project also includes an `integration/` folder with all official NBC documentation PDFs, and a `KHQR - asset/` folder with official KHQR brand assets. You can download both directly from the project repository.

---

## Notes

- **Token Auto-Renewal:** The app automatically renews the Bearer Token using your registered email. You no longer need to manually copy tokens from the Bakong portal.
- **Production Restriction:** The `check-transaction` endpoint can only be called from servers **located in Cambodia** in a production environment. Calls from servers outside Cambodia will be blocked.
- **Currency:** The example uses USD. You can change the currency to KHR by modifying the `KHQRCurrency` setting in `MerchantInfo`.
- **Customization:** The `MerchantInfo` object (merchant name, city, bill number, etc.) should be updated to match your actual business information before going to production.
- **Branding Compliance:** When displaying KHQR in any customer-facing UI, always follow the official KHQR Card Guideline and use only the approved assets from the `KHQR - asset/` folder.

---

## License

This project is for educational and integration reference purposes.