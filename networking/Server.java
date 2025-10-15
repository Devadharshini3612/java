package networking;
import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        Socket socket = null;
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            // Start server on port 5000
            serverSocket = new ServerSocket(5000);
            System.out.println("✅ Server started. Waiting for client...");

            // Accept client connection
            socket = serverSocket.accept();
            System.out.println("✅ Client connected!");

            // Input & Output streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Read message from client
            String clientMsg = in.readLine();
            if (clientMsg != null) {
                System.out.println("📩 Client says: " + clientMsg);
                // Send response
                out.println("Hello Client, I received: " + clientMsg);
            } else {
                System.out.println("⚠️ No message received from client.");
            }

        } catch (IOException e) {
            System.out.println("❌ Server error: " + e.getMessage());
        } finally {
            // Close resources safely
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
                if (serverSocket != null) serverSocket.close();
                System.out.println("🔒 Server closed.");
            } catch (IOException e) {
                System.out.println("❌ Error closed server: " + e.getMessage());
            }
        }
    }
}
