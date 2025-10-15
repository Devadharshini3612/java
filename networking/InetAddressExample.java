package networking;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class InetAddressExample {
    public static void main(String[] args) {
        try {
            // Get the local host address
            InetAddress localHost = InetAddress.getLocalHost();
            System.out.println("Local Host Name: " + localHost.getHostName());
            System.out.println("Local Host Address: " + localHost.getHostAddress());

            // Get InetAddress by name
            InetAddress google = InetAddress.getByName("www.google.com");
            System.out.println("\nHost Name: " + google.getHostName());
            System.out.println("Host Address: " + google.getHostAddress());

            // Get all IPs associated with a domain
            InetAddress[] allAddresses = InetAddress.getAllByName("www.google.com");
            System.out.println("\nAll IP Addresses of www.google.com:");
            for (InetAddress addr : allAddresses) {
                System.out.println(addr.getHostAddress());
            }

        } catch (UnknownHostException e) {
            System.out.println("Host not found: " + e.getMessage());
        }
    }
}
