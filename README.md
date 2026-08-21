Fix Zone (Backend API) is a multi-tenant Spring Boot REST API engineered to digitalize and streamline operations for vehicle service centers, powering data isolation and business workflows across Super Admins, Company Owners, Service Center Managers, and Customers.

The backend guarantees dynamic data isolation per tenant while serving complex workflows for booking allocations, spatial discovery, and secure third-party integrations.

2. Core Objectives
* Digitalization: Provide robust RESTful APIs to eliminate manual logs and paper records.
* Multi-Tenant Isolation: Enforce tenant-aware database scoping and authorization across all entities.
* Role-Based Access Control: Secure RBAC endpoints tailored for Super Admin, Owner, Manager, and Customer.
* Integrated Ecosystem: Seamless integration with payment gateways, email relays, and cloud media storage.

3. Core Roles & Scope
* Super Admin: Platform oversight (Tenant approval, global system settings, platform analytics).
* Company Owner: Multi-branch business management, financial metrics, and manager assignments.
* Service Center Manager: Station operations (Job cards, slot schedules, customer databases, invoicing).
* Customer: Vehicle garage management, slot booking, payment processing, and history tracking.

4. Technology Stack (Backend)
* Framework: Java 17 / Spring Boot 3.x
* Security: Spring Security & JWT (Stateless Authentication)
* Database & ORM: PostgreSQL / Spring Data JPA (Hibernate)
* Cloud Storage: ImageKit SDK
* Mail & Alerts: Brevo (Sendinblue) SMTP via JavaMailSender
* Payments: Stripe Connect API

5. Architecture Highlights
* Multi-Tenancy: Dynamic tenant scoping via custom request filters and tenant-aware repository queries.
* Security: Fine-grained method-level security with role and tenant claims in JWT tokens.
* Integration-Ready: Enterprise REST endpoints matching Next.js web portal and React Native mobile clients.
