package networking;
import java.net.*;

public class LocalInfo {
    public static void main(String[] args) {
        try {
            InetAddress local = InetAddress.getLocalHost();
            System.out.println("Host Name: " + local.getHostName());
            System.out.println("Host Address: " + local.getHostAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
