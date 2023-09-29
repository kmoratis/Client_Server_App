import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Vehicle {

   public static void dataMenu(Scanner scanner, int obdPacketNum) {
      try {
         InetAddress serverAddress = InetAddress.getByName("server_address_here"); // Replace with the actual server address
         int serverPort = 29078;

         while (true) {
               // Display menu options for vehicle data
               System.out.println("\nSelect a vehicle data option:");
               System.out.println("1. Engine Run Time");
               System.out.println("2. Intake Air Temperature");
               System.out.println("3. Throttle Position");
               System.out.println("4. Engine RPM");
               System.out.println("5. Vehicle Speed");
               System.out.println("6. Coolant Temperature");
               System.out.println("0. Exit");

               int userChoice = scanner.nextInt();
               scanner.nextLine(); // Consume the newline character

               if (userChoice == 0) {
                  System.out.println("Exiting the Vehicle Data menu.");
                  break;
               }

               switch (userChoice) {
                  case 1:
                     requestData(serverAddress, serverPort, "01 1F\r", "Engine Run Time (sec)", "obd_engineRT.csv", obdPacketNum);
                     break;
                  case 2:
                     requestData(serverAddress, serverPort, "01 0F\r", "Intake Air Temperature (Celsius)", "obd_airTemp.csv", obdPacketNum);
                     break;
                  case 3:
                     requestData(serverAddress, serverPort, "01 11\r", "Throttle Position (%)", "obd_throttle.csv", obdPacketNum);
                     break;
                  case 4:
                     requestData(serverAddress, serverPort, "01 0C\r", "Engine RPM (RPM)", "obd_engineRpm.csv", obdPacketNum);
                     break;
                  case 5:
                     requestData(serverAddress, serverPort, "01 0D\r", "Vehicle Speed (km/h)", "obd_vehicleSpeed.csv", obdPacketNum);
                     break;
                  case 6:
                     requestData(serverAddress, serverPort, "01 05\r", "Coolant Temperature (Celsius)", "obd_coolantTemp.csv", obdPacketNum);
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

   private static void requestData(InetAddress serverAddress, int serverPort, String request, String dataLabel, String fileName, int packetNum) {
      try (Socket tcpSocket = new Socket(serverAddress, serverPort)) {
         OutputStream writer = tcpSocket.getOutputStream();
         BufferedReader reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));

         System.out.println(dataLabel + ": ");
         FileWriter dataWriter = new FileWriter(fileName);
         int counter = 1;

         for (int j = 0; j < packetNum; j++) {
               writer.write(request.getBytes());
               String line = reader.readLine();

               int xValue = Integer.parseInt(line.substring(6, 8), 16);
               float floatValue = 0f;

               switch (dataLabel) {
                  case "Engine Run Time (sec)":
                     floatValue = xValue * 256 + Integer.parseInt(line.substring(9, 11), 16);
                     break;
                  case "Intake Air Temperature (Celsius)":
                     floatValue = xValue - 40;
                     break;
                  case "Throttle Position (%)":
                     floatValue = xValue * 100f / 255f;
                     break;
                  case "Engine RPM (RPM)":
                     floatValue = xValue * 256 + Integer.parseInt(line.substring(9, 11), 16);
                     floatValue /= 4;
                     break;
                  case "Vehicle Speed (km/h)":
                     floatValue = xValue;
                     break;
                  case "Coolant Temperature (Celsius)":
                     floatValue = xValue - 40;
                     break;
               }

               dataWriter.append(String.join(",", String.valueOf((int) floatValue)));
               dataWriter.append("\n");

               System.out.println("Request " + counter + ": " + line + ", Formula result = " + (int) floatValue);
               counter++;
         }

         dataWriter.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Method for downloading data from the vehicle's function, finding requested values,
    * and saving them to a .csv file.
    *
    * @param selection  The selected data request.
    * @param packetNum  The number of data packets to retrieve.
    * @throws IOException If an I/O error occurs.
    */
   public static void data(int selection, int packetNum) throws IOException {
      int serverPort = 29078;
      int xValue = 0, yValue = 0, byteFirstDig = 0, byteSecondDig = 0;
      long value = 0;
      char helpChar;
      char[] hexNums = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
      byte[] hostIp = {(byte) 155, (byte) 207, (byte) 18, (byte) 208};
      InetAddress hostAddress = InetAddress.getByAddress(hostIp);

      if (selection == 1) {
         System.out.println("Engine run time (sec): ");
         String engineRTReq = "01 1F\r";
         FileWriter engineRTWriter = new FileWriter("obd_engineRT.csv");
         int counter = 1;

         try (Socket tcpSocket = new Socket(hostAddress, serverPort)) {
               OutputStream writer = tcpSocket.getOutputStream();
               BufferedReader reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
               String line;

               do {
                  writer.write(engineRTReq.getBytes());
                  line = reader.readLine();

                  helpChar = line.toCharArray()[6];
                  // Finds the decimal value of the hex digit.
                  for (int i = 0; i < hexNums.length; i++) {
                     if (helpChar == hexNums[i]) {
                           byteFirstDig = i;
                           break;
                     }
                  }

                  helpChar = line.toCharArray()[7];
                  for (int i = 0; i < hexNums.length; i++) {
                     if (helpChar == hexNums[i]) {
                           byteSecondDig = i;
                           break;
                     }
                  }

                  xValue = byteFirstDig * 16 + byteSecondDig;

                  helpChar = line.toCharArray()[9];
                  for (int i = 0; i < hexNums.length; i++) {
                     if (helpChar == hexNums[i]) {
                           byteFirstDig = i;
                           break;
                     }
                  }

                  helpChar = line.toCharArray()[10];
                  for (int i = 0; i < hexNums.length; i++) {
                     if (helpChar == hexNums[i]) {
                           byteSecondDig = i;
                           break;
                     }
                  }

                  yValue = byteFirstDig * 16 + byteSecondDig;
                  value = xValue * 256 + yValue;
                  engineRTWriter.append(String.join(",", String.valueOf(value)));
                  engineRTWriter.append("\n");
                  System.out.println("Request " + counter + ": " + line + ", Formula result = " + value);
                  counter++;
               } while (value < 240); // 240 seconds = 4 minutes
         }
         System.out.println("Packets needed for 4 min: " + (counter - 1));
         engineRTWriter.close();
      } else if (selection == 2) {
         // Similar code blocks for other selections...
      }
   }

   public static void main(String[] args) {
      try {
         data(1, 10); // Example usage: retrieve engine run time data for 10 packets.
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
