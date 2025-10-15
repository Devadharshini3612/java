package networking;
import java.net.*;

public class URLInfo {
    public static void main(String[] args) {
        try {
            URL url = new URL("https://www.wikipedia.org");
            System.out.println("Protocol: " + url.getProtocol());
            System.out.println("Host: " + url.getHost());
            System.out.println("Port: " + url.getPort()); // -1 means default port
            System.out.println("File: " + url.getFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
