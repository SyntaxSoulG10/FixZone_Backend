# FixZone API Documentation

This document lists the primary API endpoints for the FixZone system. The backend runs on `http://localhost:8081`.

---

## 1. Authentication Endpoints (`/api/auth`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| POST | `/api/auth/register` | Create a new customer or service center account. |
| POST | `/api/auth/login` | Authenticate and receive a JWT token. |
| POST | `/api/auth/logout` | Terminate the current session. |

---

## 2. Booking Endpoints (`/api/bookings`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| POST | `/api/bookings` | Create a new service booking. |
| GET | `/api/bookings/my-bookings` | Retrieve bookings for the logged-in customer. |
| PUT | `/api/bookings/{id}/reschedule` | Update the date/time of an existing booking. |
| GET | `/api/bookings/available-slots` | Check availability for a specific center and date. |

---

## 3. Payment Endpoints (`/api/payments`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| POST | `/api/payments/create-intent` | Initialize a Stripe payment intent. |
| POST | `/api/payments/confirm` | Confirm successful payment and update booking status. |
| GET | `/api/payments/history` | View transaction history for the current user. |

---

## 4. User & Profile Endpoints (`/api/users`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| GET | `/api/users/profile` | Get current user details. |
| PUT | `/api/users/profile` | Update user information. |

---

## 5. Service Centers (`/api/service-centers`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| GET | `/api/service-centers` | List all registered service centers. |
| GET | `/api/service-centers/{id}` | Get detailed info for a specific center. |
| GET | `/api/service-centers/search` | Search centers by location or name. |

---

### Technical Notes:
- **Base URL:** `http://localhost:8081`
- **Authentication:** Bearer JWT in the `Authorization` header.
- **Data Format:** JSON (Request/Response).
- **Validation:** Spring Boot `@Valid` annotation used for all POST/PUT requests.
