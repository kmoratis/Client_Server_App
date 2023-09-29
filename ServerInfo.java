import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * A class for sending a server's information request and receiving the server's response.
 */
public class ServerInfo {

   public static void infoMenu(int serverPort, int clientPort) {
      try (Scanner scanner = new Scanner(System.in)) {
         InetAddress hostAddress = InetAddress.getByName("server_address_here"); // Replace with the actual server address

         while (true) {
               // Display menu options for server info
               System.out.println("\nSelect a server info option:");
               System.out.println("1. Get Server Information");
               System.out.println("0. Exit");

               int userChoice = scanner.nextInt();
               scanner.nextLine(); // Consume the newline character

               if (userChoice == 0) {
                  System.out.println("Exiting the Server Info menu.");
                  break;
               }

               switch (userChoice) {
                  case 1:
                     requestServerInfo(hostAddress, serverPort, clientPort);
                     break;
                  default:
                     System.out.println("Invalid input. Please try again.");
                     break;
               }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private static void requestServerInfo(InetAddress hostAddress, int serverPort, int clientPort) {
      try (Socket outSocket = new Socket(hostAddress, serverPort)) {
         PrintWriter writer = new PrintWriter(outSocket.getOutputStream(), true);
         BufferedReader reader = new BufferedReader(new InputStreamReader(outSocket.getInputStream()));

         String serverRequest = "GET /index.html HTTP/1.0\r\n\r\n";
         writer.println(serverRequest);

         System.out.println("Server Information:");

         String line;
         while ((line = reader.readLine()) != null) {
               System.out.println(line);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * Sends a server's information request and receives the server's response.
    *
    * @throws IOException If an I/O error occurs.
    */
   public static void info() throws IOException {
      int serverPort = 80;

      // Define the HTTP request to send to the server.
      String serverRequest = "GET /index.html HTTP/1.0\\r\\n\\r\\n";

      // Define the host's IP address.
      byte[] hostIp = {(byte) 155, (byte) 207, (byte) 18, (byte) 208};
      InetAddress hostAddress = InetAddress.getByAddress(hostIp);

      try (Socket outSocket = new Socket(hostAddress, serverPort)) {
         PrintWriter writer = new PrintWriter(outSocket.getOutputStream(), true);

         // Send the HTTP request to the server.
         writer.println(serverRequest.getBytes());

         BufferedReader reader = new BufferedReader(
                  new InputStreamReader(outSocket.getInputStream()));
         String line;

         // Read and print the server's response.
         while ((line = reader.readLine()) != null) {
               System.out.println(line);
         }
      }
   }


   public static void main(String[] args) {
      try {
         info(); // Example usage: send a server info request and display the response.
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
