package com.fixzone.fixzon_backend;

import org.junit.jupiter.api.Test;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class DirectSeedTodayBookingsTest {

    @Test
    public void seedManager1TodayBookingsDirectly() throws Exception {
        String url = "jdbc:postgresql://ep-delicate-credit-anm2vgw4.c-6.us-east-1.aws.neon.tech/neondb?sslmode=require&tcpKeepAlive=true";
        String user = "neondb_owner";
        String pass = "npg_IrqtQBJ4TcN5";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            conn.setAutoCommit(false);

            // 1. Get real valid Service Center from database
            UUID centerId = null;
            UUID ownerId = null;
            String centerName = "";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT center_id, owner_id, name FROM service_centers ORDER BY created_at ASC LIMIT 1")) {
                if (rs.next()) {
                    centerId = (UUID) rs.getObject("center_id");
                    ownerId = (UUID) rs.getObject("owner_id");
                    centerName = rs.getString("name");
                }
            }

            if (centerId == null) {
                throw new RuntimeException("No service center found in database!");
            }
            System.out.println("Valid Service Center: " + centerName + " (" + centerId + ") Owned by: " + ownerId);

            // 2. Assign manager1@fixzone.lk to this valid center
            UUID managerId = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE email = 'manager1@fixzone.lk'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) managerId = (UUID) rs.getObject("user_id");
                }
            }

            if (managerId == null) {
                managerId = UUID.randomUUID();
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users (user_id, email, full_name, phone, role, status, password_hash, created_at, updated_at) " +
                        "VALUES (?, 'manager1@fixzone.lk', 'Roshan Wijesinghe', '+94772000000', 'ROLE_SERVICE_MANAGER', 'Active', '$2a$10$w8T06Tz4Z8fUvGvG6n6jPe2mG3o1.6wN5I2y0P2eQ.f9Y3aP3m2K', NOW(), NOW()) " +
                        "ON CONFLICT (email) DO UPDATE SET is_active=true RETURNING user_id")) {
                    ps.setObject(1, managerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) managerId = (UUID) rs.getObject(1);
                    }
                }
            }

            // Always update manager's managed_center_id to the real centerId
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO service_center_manager (user_id, manager_code, managed_center_id) VALUES (?, 'MGR-001', ?) " +
                    "ON CONFLICT (user_id) DO UPDATE SET managed_center_id = EXCLUDED.managed_center_id")) {
                ps.setObject(1, managerId);
                ps.setObject(2, centerId);
                ps.executeUpdate();
            }

            System.out.println("Assigned manager1 (" + managerId + ") -> center: " + centerName + " (" + centerId + ")");

            // 3. Ensure 2 Customer accounts exist
            UUID cust1Id = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (user_id, email, full_name, phone, role, status, password_hash, created_at, updated_at) " +
                    "VALUES (?, 'kamal.perera@fixzone.lk', 'Kamal Perera', '+94771234567', 'ROLE_CUSTOMER', 'Active', '$2a$10$w8T06Tz4Z8fUvGvG6n6jPe2mG3o1.6wN5I2y0P2eQ.f9Y3aP3m2K', NOW(), NOW()) " +
                    "ON CONFLICT (email) DO UPDATE SET full_name = EXCLUDED.full_name RETURNING user_id")) {
                ps.setObject(1, UUID.randomUUID());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) cust1Id = (UUID) rs.getObject(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO customers (user_id, customer_code, preferred_contact_method, visits, total_spent) " +
                    "VALUES (?, 'CUST-KP01', 'PHONE', 1, 14500.00) " +
                    "ON CONFLICT (user_id) DO NOTHING")) {
                ps.setObject(1, cust1Id);
                ps.executeUpdate();
            }

            UUID cust2Id = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (user_id, email, full_name, phone, role, status, password_hash, created_at, updated_at) " +
                    "VALUES (?, 'nimal.silva@fixzone.lk', 'Nimal Silva', '+94779876543', 'ROLE_CUSTOMER', 'Active', '$2a$10$w8T06Tz4Z8fUvGvG6n6jPe2mG3o1.6wN5I2y0P2eQ.f9Y3aP3m2K', NOW(), NOW()) " +
                    "ON CONFLICT (email) DO UPDATE SET full_name = EXCLUDED.full_name RETURNING user_id")) {
                ps.setObject(1, UUID.randomUUID());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) cust2Id = (UUID) rs.getObject(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO customers (user_id, customer_code, preferred_contact_method, visits, total_spent) " +
                    "VALUES (?, 'CUST-NS02', 'EMAIL', 1, 9500.00) " +
                    "ON CONFLICT (user_id) DO NOTHING")) {
                ps.setObject(1, cust2Id);
                ps.executeUpdate();
            }

            // 4. Ensure Vehicles exist
            UUID veh1Id = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO vehicles (id, customer_id, brand, model, plate_number, vehicle_type, last_service_date) " +
                    "VALUES (?, ?, 'Toyota', 'Prius', 'WP CAB-4521', 'CAR', CURRENT_DATE - INTERVAL '90 days') " +
                    "ON CONFLICT (plate_number) DO UPDATE SET customer_id = EXCLUDED.customer_id, brand = EXCLUDED.brand, model = EXCLUDED.model RETURNING id")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, cust1Id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) veh1Id = (UUID) rs.getObject(1);
                }
            }

            UUID veh2Id = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO vehicles (id, customer_id, brand, model, plate_number, vehicle_type, last_service_date) " +
                    "VALUES (?, ?, 'Honda', 'Vezel', 'WP CAD-7890', 'CAR', CURRENT_DATE - INTERVAL '120 days') " +
                    "ON CONFLICT (plate_number) DO UPDATE SET customer_id = EXCLUDED.customer_id, brand = EXCLUDED.brand, model = EXCLUDED.model RETURNING id")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, cust2Id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) veh2Id = (UUID) rs.getObject(1);
                }
            }

            // 5. Ensure Service Packages exist for this valid center
            UUID pkg1Id = null;
            UUID pkg2Id = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT package_id FROM service_packages WHERE center_id = ? LIMIT 2")) {
                ps.setObject(1, centerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) pkg1Id = (UUID) rs.getObject("package_id");
                    if (rs.next()) pkg2Id = (UUID) rs.getObject("package_id");
                }
            }

            if (pkg1Id == null) {
                pkg1Id = UUID.randomUUID();
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO service_packages (package_id, center_id, name, type, vehicle_brand, vehicle_type, description, base_price, estimated_duration_mins, is_active, created_at, updated_at) " +
                        "VALUES (?, ?, 'Full Hybrid Periodic Service', 'Maintenance', 'Toyota', 'CAR', 'Complete lube, filter, brake scan, and battery inspection.', 14500.00, 90, true, NOW(), NOW())")) {
                    ps.setObject(1, pkg1Id);
                    ps.setObject(2, centerId);
                    ps.executeUpdate();
                }
            }
            if (pkg2Id == null) {
                pkg2Id = UUID.randomUUID();
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO service_packages (package_id, center_id, name, type, vehicle_brand, vehicle_type, description, base_price, estimated_duration_mins, is_active, created_at, updated_at) " +
                        "VALUES (?, ?, 'Standard Periodic Maintenance & Inspection', 'Inspection', 'Honda', 'CAR', 'Engine oil replacement, brake check, multi-point diagnostic check.', 9500.00, 60, true, NOW(), NOW())")) {
                    ps.setObject(1, pkg2Id);
                    ps.setObject(2, centerId);
                    ps.executeUpdate();
                }
            }

            // 6. Delete old today bookings for this center
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM booking_status_history WHERE booking_id IN (SELECT booking_id FROM bookings WHERE center_id = ? AND booking_date = CURRENT_DATE)")) {
                ps.setObject(1, centerId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM bookings WHERE center_id = ? AND booking_date = CURRENT_DATE")) {
                ps.setObject(1, centerId);
                ps.executeUpdate();
            }

            // 7. Insert 2 Today Bookings: 1 IN_PROGRESS and 1 CONFIRMED
            UUID b1Id = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO bookings (booking_id, tenant_id, center_id, customer_id, vehicle_id, package_id, booking_date, booking_time, status, estimated_cost, booking_fee, booking_fee_paid, special_request, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, '09:30:00', 'IN_PROGRESS', 14500.00, 2000.00, true, 'Customer: Kamal Perera, Vehicle: Toyota Prius, Vehicle Number: WP CAB-4521, Service: Full Hybrid Periodic Service', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '45 minutes')")) {
                ps.setObject(1, b1Id);
                ps.setObject(2, ownerId != null ? ownerId : UUID.randomUUID());
                ps.setObject(3, centerId);
                ps.setObject(4, cust1Id);
                ps.setObject(5, veh1Id);
                ps.setObject(6, pkg1Id);
                ps.executeUpdate();
            }

            // Status history for b1
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO booking_status_history (id, booking_id, status, changed_at, changed_by) VALUES (?, ?, ?, ?, ?)")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, b1Id);
                ps.setString(3, "PENDING_PAYMENT");
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().minusHours(3)));
                ps.setString(5, "CUSTOMER");
                ps.executeUpdate();

                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, b1Id);
                ps.setString(3, "CONFIRMED");
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().minusHours(2)));
                ps.setString(5, "CUSTOMER_PAYMENT");
                ps.executeUpdate();

                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, b1Id);
                ps.setString(3, "IN_PROGRESS");
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().minusMinutes(45)));
                ps.setString(5, "MANAGER");
                ps.executeUpdate();
            }

            UUID b2Id = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO bookings (booking_id, tenant_id, center_id, customer_id, vehicle_id, package_id, booking_date, booking_time, status, estimated_cost, booking_fee, booking_fee_paid, special_request, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, '14:00:00', 'CONFIRMED', 9500.00, 1500.00, true, 'Customer: Nimal Silva, Vehicle: Honda Vezel, Vehicle Number: WP CAD-7890, Service: Standard Periodic Maintenance & Inspection', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour')")) {
                ps.setObject(1, b2Id);
                ps.setObject(2, ownerId != null ? ownerId : UUID.randomUUID());
                ps.setObject(3, centerId);
                ps.setObject(4, cust2Id);
                ps.setObject(5, veh2Id);
                ps.setObject(6, pkg2Id);
                ps.executeUpdate();
            }

            // Status history for b2
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO booking_status_history (id, booking_id, status, changed_at, changed_by) VALUES (?, ?, ?, ?, ?)")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, b2Id);
                ps.setString(3, "PENDING_PAYMENT");
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().minusHours(2)));
                ps.setString(5, "CUSTOMER");
                ps.executeUpdate();

                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, b2Id);
                ps.setString(3, "CONFIRMED");
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().minusHours(1)));
                ps.setString(5, "CUSTOMER_PAYMENT");
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("=================================================================");
            System.out.println(">>> SUCCESS: 2 TODAY'S BOOKINGS SEEDED DIRECTLY FOR MANAGER1! <<<");
            System.out.println("Center: " + centerName + " (" + centerId + ")");
            System.out.println("1) Active [IN_PROGRESS]: " + b1Id + " | Kamal Perera | Toyota Prius (WP CAB-4521) | 09:30");
            System.out.println("2) Upcoming [CONFIRMED]: " + b2Id + " | Nimal Silva | Honda Vezel (WP CAD-7890) | 14:00");
            System.out.println("=================================================================");
        }
    }
}
