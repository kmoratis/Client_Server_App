import java.io.FileWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class IthakiCopter {

   public static void telemetryMenu(int serverPort, int clientPort) {
      try {
         InetAddress serverAddress = InetAddress.getByName("server_address_here"); // Replace with the actual server address

         DatagramSocket clientSocket = new DatagramSocket(clientPort);

         boolean offGround = false, landed = false, firstTime = true;
         String lmotor = "", rmotor = "", altitude = "";

         while (!landed) {
               // Get user input (you can change this as needed)
               System.out.print("Press Enter to request telemetry data (or type 'exit' to exit): ");
               Scanner scanner = new Scanner(System.in);
               String userInput = scanner.nextLine();

               if ("exit".equalsIgnoreCase(userInput)) {
                  break;
               }

               // Send the telemetry request to the server
               String copterRequest = "COPTER " + lmotor + " " + rmotor + " " + altitude + "\r\n";
               byte[] sendData = copterRequest.getBytes();
               DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
               clientSocket.send(sendPacket);

               // Receive and process the telemetry data from the server
               byte[] receiveData = new byte[1024]; // Adjust buffer size as needed
               DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
               clientSocket.receive(receivePacket);
               String telemetryData = new String(receivePacket.getData(), 0, receivePacket.getLength());

               // Parse and process the telemetry data (adjust as needed)
               lmotor = telemetryData.substring(40, 43);
               rmotor = telemetryData.substring(51, 54);
               altitude = telemetryData.substring(64, 67);

               if (Integer.valueOf(altitude) > 80 && firstTime) {
                  System.out.println("Copter took off");
                  offGround = true;
                  firstTime = false;
               }

               if (offGround && Integer.valueOf(altitude) == 75) {
                  System.out.println("Copter landed");
                  landed = true;
               }

               // Display or process telemetry data as needed
               System.out.println("Telemetry Data: " + telemetryData);
         }
         clientSocket.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Method for receiving telemetry packets from the copter and saving the requested values to a .csv file.
    *
    * @throws Exception If an error occurs during communication or file writing.
    */
   public static void telemetry() throws Exception {
      int clientPort = 48078;
      boolean offGround = false, landed = false, firstTime = true;
      String message = "", lmotor = "", rmotor = "", altitude = "";

      // Create a CSV file for storing telemetry data.
      FileWriter csvWriter = new FileWriter("ithakiCopterG20u.csv");

      // Write header row to the CSV file.
      csvWriter.append(String.join(",", "lmotor", "rmotor", "altitude"));
      csvWriter.append("\n");

      // Create a UDP socket for receiving telemetry data.
      DatagramSocket inSocket = new DatagramSocket(clientPort);
      inSocket.setSoTimeout(20000);

      byte[] inBuffer = new byte[2048];
      DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);

      while (!landed) {
         try {
               // Receive a telemetry packet from the copter.
               inSocket.receive(inPacket);
               message = new String(inBuffer, 0, inPacket.getLength());
         } catch (Exception e) {
               System.out.println(e);
         }

         System.out.println(message);

         lmotor = message.substring(40, 43);
         rmotor = message.substring(51, 54);
         altitude = message.substring(64, 67);

         if (Integer.valueOf(altitude) > 80 && firstTime) {
               System.out.println("Copter took off");
               offGround = true;
               firstTime = false;
         }

         // Stop receiving when the copter lands (reaches an altitude of 75).
         if (offGround && Integer.valueOf(altitude) == 75) {
               System.out.println("Copter landed");
               landed = true;
         }

         // Write telemetry data to the CSV file.
         csvWriter.append(String.join(",", lmotor, rmotor, altitude));
         csvWriter.append("\n");
      }

      // Close the UDP socket and CSV file.
      inSocket.close();
      csvWriter.close();
   }
}
