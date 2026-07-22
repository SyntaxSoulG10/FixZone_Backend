
package com.fixzone.fixzon_backend;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class FixDbTest {
    @Test
    public void fixDatabase() throws Exception {
        String url = "jdbc:postgresql://ep-sparkling-river-an87fzac-pooler.c-6.us-east-1.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_IrqtQBJ4TcN5";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
            System.out.println("Executing ALTER TABLE for bookings...");
            stmt.execute("ALTER TABLE IF EXISTS bookings ALTER COLUMN booking_id TYPE uuid USING booking_id::uuid");
            System.out.println("Successfully altered bookings.booking_id!");
            
            System.out.println("Executing ALTER TABLE for booking_history...");
            stmt.execute("ALTER TABLE IF EXISTS booking_history ALTER COLUMN booking_id TYPE uuid USING booking_id::uuid");
            System.out.println("Successfully altered booking_history.booking_id!");
            
            // Just in case tenant_id, center_id, etc were also changed in bookings:
            // But we'll stick to booking_id since that was the confirmed change.
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
