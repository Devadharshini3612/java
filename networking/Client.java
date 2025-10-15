package networking;
import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        Socket socket = null;
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            // Connect to server at localhost:5000
            socket = new Socket("localhost", 5000);
            System.out.println("✅ Connected to server!");

            // Input & Output streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send message to server
            out.println("Hello Server, this is Client!");

            // Read server response
            String serverMsg = in.readLine();
            if (serverMsg != null) {
                System.out.println("📩 Server says: " + serverMsg);
            } else {
                System.out.println("⚠️ No response from server.");
            }

        } catch (UnknownHostException e) {
            System.out.println("❌ Unknown Host: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("❌ I/O Error: " + e.getMessage());
        } finally {
            // Close resources safely
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
                System.out.println("🔒 Client closed.");
            } catch (IOException e) {
                System.out.println("❌ Error closed client: " + e.getMessage());
            }
        }
    }
}
