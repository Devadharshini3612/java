package manual;

import java.sql.*;

public class MySQLConnect {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/STUDENT?useSSL=false&serverTimezone=UTC";
        String user = "root";
        String password = "dharshini3612@";

        System.out.println("TC1: Connecting to database...");
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Output: Connected ✅");

            Statement stmt = conn.createStatement();

            // Test Case 2: SELECT query
            System.out.println("\nTC2: Executing SELECT query...");
            ResultSet rs = stmt.executeQuery("SELECT * FROM Student");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println(
                    rs.getInt("s_id") + " | " + rs.getString("s_name") + " | " + rs.getString("s_dept"));
            }
            if (!found)
                System.out.println("Output: No records found ❌");
            else
                System.out.println("Output: Records displayed ✅");

            // Test Case 4: Empty query
            System.out.println("\nTC4: Executing empty query...");
            try {
                stmt.execute("");
                System.out.println("Output: No results ✅");
            } catch (SQLException e) {
                System.out.println("Output: " + e.getMessage());
            }

            // Test Case 5: Malformed SQL
            System.out.println("\nTC5: Executing malformed SQL...");
            try {
                stmt.executeQuery("SELEC * FRM Student");
            } catch (SQLException e) {
                System.out.println("Output: SQLSyntaxErrorException ✅ (" + e.getMessage() + ")");
            }

        } catch (SQLException e) {
            if (e.getMessage().contains("Access denied")) {
                System.out.println("TC3: Invalid credentials → Output: Access denied ✅");
            } else {
                System.out.println("SQL Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
