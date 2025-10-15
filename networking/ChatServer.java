package networking;
import java.io.*;
import java.net.*;

public class ChatServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("✅ Server started. Waiting for client...");

            try (Socket socket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {

                System.out.println("✅ Client connected!");

                // Thread for reading messages from client
                Thread readThread = new Thread(() -> {
                    try {
                        String msg;
                        while ((msg = in.readLine()) != null) {
                            if (msg.equalsIgnoreCase("exit")) {
                                System.out.println("❌ Client left the chat.");
                                break;
                            }
                            System.out.println("📩 Client: " + msg);
                        }
                    } catch (IOException e) {
                        System.out.println("❌ Error reading from client: " + e.getMessage());
                    }
                });

                readThread.start();

                // Main thread handles sending messages
                String sendMsg;
                while ((sendMsg = consoleInput.readLine()) != null) {
                    out.println(sendMsg);
                    if (sendMsg.equalsIgnoreCase("exit")) {
                        System.out.println("🔒 Closing chat...");
                        break;
                    }
                }

            } catch (IOException e) {
                System.out.println("❌ Server I/O error: " + e.getMessage());
            }

        } catch (IOException e) {
            System.out.println("❌ Could not start server: " + e.getMessage());
        }
    }
}
