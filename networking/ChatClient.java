package networking;
import java.io.*;
import java.net.*;

public class ChatClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 5000);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("✅ Connected to server. You can start chatting... (type 'exit' to quit)");

            // Thread for reading messages from server
            Thread readThread = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        if (msg.equalsIgnoreCase("exit")) {
                            System.out.println("❌ Server ended the chat.");
                            break;
                        }
                        System.out.println("📩 Server: " + msg);
                    }
                } catch (IOException e) {
                    System.out.println("❌ Error reading from server: " + e.getMessage());
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

        } catch (UnknownHostException e) {
            System.out.println("❌ Unknown host: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("❌ I/O error: " + e.getMessage());
        }
    }
}
