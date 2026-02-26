# Bakong API Integration with Spring Boot

A Spring Boot project demonstrating how to integrate with the **Bakong Open API** and **KHQR SDK** to generate QR codes for payments and verify transactions.

---

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Resources & Documentation](#resources--documentation)
- [Project Setup](#project-setup)
- [Dependencies](#dependencies)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Usage Guide](#usage-guide)
- [Notes](#notes)

---

## Prerequisites

Before starting the integration, you need to:

1. **Create a Bakong Account** — Register at [https://bakong.nbc.gov.kh](https://bakong.nbc.gov.kh) and get your account verified.
2. **Register with Bakong Open API** — Sign up with your email at [https://api-bakong.nbc.gov.kh/](https://api-bakong.nbc.gov.kh/) to receive your **Bearer Token** for API access.

---

## Resources & Documentation

Read these documents before starting the integration:

| Resource | Link |
|---|---|
| Bakong Open API Documentation | [Download PDF](https://bakong.nbc.gov.kh/download/KHQR/integration/Bakong%20Open%20API%20Document.pdf) |
| KHQR SDK Documentation | [Download PDF](https://bakong.nbc.gov.kh/download/KHQR%20SDK.pdf) |
| Bakong Open API Portal | [https://api-bakong.nbc.gov.kh/](https://api-bakong.nbc.gov.kh/) |

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
bakong.bearer-token=${BAKONG_BEARER_TOKEN}
```

### `application-dev.properties`

```properties
BAKONG_ACCOUNT_ID=your_bakong_id@bank
ACQUIRINGBANK=your_acquiring_bank_code
MERCHANTNAME=Your-Merchant-Name
MOBILENUMBERSTORELABEL=YourLabel
STORELABEL=YourStoreLabel
BAKONG_BASE_URL=https://api-bakong.nbc.gov.kh
BAKONG_BEARER_TOKEN=your_jwt_bearer_token_here
```

> ⚠️ **Security Warning:** Never commit real tokens or credentials to version control. Use environment variables or a secrets manager in production.

---

## API Endpoints

All endpoints are prefixed with `/api/v1/bakong`.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/generate-qr` | Generates a KHQR string and MD5 hash |
| `GET` | `/get-qr-image` | Converts a KHQR string into a scannable PNG image |
| `POST` | `/check-transaction` | Checks if a transaction has been completed using its MD5 hash |

---

## Usage Guide

Follow these steps in order to complete a payment flow:

### Step 1 — Generate QR Code

Call the `generate-qr` endpoint with the payment amount. This returns a KHQR string and an MD5 hash.

**Request:**

```
GET http://localhost:8080/api/v1/bakong/generate-qr
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
GET http://localhost:8080/api/v1/bakong/get-qr-image
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

**Request:**

```
POST http://localhost:8080/api/v1/bakong/check-transaction
Content-Type: application/json

{
    "md5": "2e8787edaddc31ffe9c572923db06d33"
}
```

**Response — Payment Successful:**

```json
{
    "responseCode": 0,
    "responseMessage": "Success",
    "errorCode": null,
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

**Response — Payment Not Found:**

```json
{
    "responseCode": 1,
    "responseMessage": "Transaction could not be found. Please check and try again.",
    "errorCode": 1,
    "data": null
}
```

---

## Notes

- **Token Expiry:** The Bearer Token obtained from the Bakong Open API portal has an expiration date. Make sure to renew it before it expires to avoid `403 Forbidden` errors.
- **Production Restriction:** The `check-transaction` endpoint can only be called from servers **located in Cambodia** in a production environment. Calls from servers outside Cambodia will be blocked.
- **Currency:** The example uses USD. You can change the currency to KHR by modifying the `KHQRCurrency` setting in `MerchantInfo`.
- **Customization:** The `MerchantInfo` object (merchant name, city, bill number, etc.) should be updated to match your actual business information before going to production.

---

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com.tongbora.bakongapiintergration/
│   │       ├── config/
│   │       │   ├── JacksonConfig.java          # Jackson ObjectMapper configuration
│   │       │   └── RestTemplateConfig.java     # RestTemplate bean configuration
│   │       ├── controller/
│   │       │   └── BakongController.java       # REST API endpoints
│   │       ├── dto/
│   │       │   └── BakongRequest.java          # Request DTO (amount)
│   │       ├── service/
│   │       │   ├── BakongService.java          # Service interface
│   │       │   └── impl/
│   │       │       └── BakongServiceImpl.java  # Business logic implementation
│   │       └── BakongApiIntergrationApplication.java  # Spring Boot entry point
│   └── resources/
│       ├── static/
│       ├── templates/
│       ├── application.properties
│       └── application-dev.properties
└── test/
```

---

## License

This project is for educational and integration reference purposes.