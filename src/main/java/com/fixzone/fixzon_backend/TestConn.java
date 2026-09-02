package com.fixzone.fixzon_backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestConn {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://ep-billowing-king-a1ez4i2v-pooler.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_jX9VpsAHb8zE";
        
        try {
            System.out.println("Connecting...");
            Connection conn = DriverManager.getConnection(url, user, pass);
            System.out.println("Connected!");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");
            if (rs.next()) {
                System.out.println("Test query result: " + rs.getInt(1));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
