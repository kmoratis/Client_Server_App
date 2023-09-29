import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class EchoPacket {

   public static void echoMenu(int serverPort, int clientPort, String echoRequest, Scanner scanner) {
      try {
         InetAddress serverAddress = InetAddress.getByName("server_address_here"); // Replace with the actual server address

         DatagramSocket clientSocket = new DatagramSocket(clientPort);

         while (true) {
               // Get user input
               System.out.print("Enter a message to send (or type 'exit' to exit): ");
               String userInput = scanner.nextLine();

               if ("exit".equalsIgnoreCase(userInput)) {
                  break;
               }

               // Send the echo request to the server
               byte[] sendData = userInput.getBytes();
               DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
               clientSocket.send(sendPacket);

               // Receive the response from the server
               byte[] receiveData = new byte[1024];
               DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
               clientSocket.receive(receivePacket);

               String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
               System.out.println("Server Response: " + receivedMessage);
         }

         clientSocket.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }


   /**
    * Downloads echo packets for 4 minutes, finds the time needed to download each packet,
    * and creates a CSV file for diagram G1.
    *
    * @param serverPort      The server's port for sending echo requests.
    * @param clientPort      The client's port for receiving echo responses.
    * @param echoRequestCode The echo request code.
    * @param delay           Whether to include delays in the statistics.
    * @param temp            Whether to include temperature information in the request.
    * @throws Exception If an error occurs during the process.
    */
   public static void echo(int serverPort, int clientPort, String echoRequestCode, boolean delay, boolean temp)
         throws Exception {
      // File names for CSV output
      String echoCsvName = "echoG1.csv";
      String echoName2 = "echoG2.csv";
      String r1Name = "r1Diagram.csv";

      int counter = 0, hour = 0, minute = 0, second = 0;
      char[] secondArr = new char[2];
      char[] minArr = new char[2];
      char[] hourArr = new char[2];
      long elapsedTime = 0, startClock = 0, secondFull = 0, runFor = 4;
      boolean timeout = false;
      long startFourMin = System.currentTimeMillis();
      List<String> timeList = new ArrayList<String>();
      ArrayList<Long> secondList = new ArrayList<Long>();
      ArrayList<Long> elapsedList = new ArrayList<Long>();

      DatagramSocket outSocket = new DatagramSocket();

      if (temp) {
         String tempCode = "T00";
         echoRequestCode += tempCode;
      }

      String message = "", prevMessage = "";
      byte[] outBuffer = echoRequestCode.getBytes();
      byte[] hostIp = {(byte) 155, (byte) 207, (byte) 18, (byte) 208};
      InetAddress hostAddress = InetAddress.getByAddress(hostIp);
      DatagramPacket outPacket = new DatagramPacket(outBuffer, outBuffer.length, hostAddress, serverPort);

      DatagramSocket inSocket = new DatagramSocket(clientPort);
      inSocket.setSoTimeout(8000);
      byte[] inBuffer = new byte[2048];
      DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);

      try {
         do {
               counter++;

               if (!timeout) {
                  startClock = System.currentTimeMillis();
               }

               outSocket.send(outPacket);
               System.out.println("Request: " + counter + " sent");

               try {
                  inSocket.receive(inPacket);
                  message = new String(inBuffer, 0, inPacket.getLength());
               } catch (Exception x) {
                  System.out.println(x);
               }

               if (message.equals(prevMessage)) {
                  System.out.println("SoTimeOut reached. Re-sending the request");
                  timeout = true;
                  counter--;
                  continue;
               }

               prevMessage = message;
               timeout = false;
               System.out.println(message);

               hourArr[0] = message.toCharArray()[18];
               hourArr[1] = message.toCharArray()[19];
               hour = Integer.parseInt(new String(hourArr));

               minArr[0] = message.toCharArray()[21];
               minArr[1] = message.toCharArray()[22];
               minute = Integer.parseInt(new String(minArr));

               secondArr[0] = message.toCharArray()[24];
               secondArr[1] = message.toCharArray()[25];
               second = Integer.parseInt(new String(secondArr));
               secondFull = (hour * 3600) + (minute * 60) + second;
               secondList.add(secondFull);

               elapsedTime = System.currentTimeMillis() - startClock;
               timeList.add(String.valueOf(elapsedTime));
               elapsedList.add(elapsedTime);

               System.out.println("packet's second: " + secondFull);
         } while (System.currentTimeMillis() - startFourMin < (runFor * 1000 * 60));

         // Create CSV files
         EchoPacket.toCsvFile(timeList, echoCsvName);
         EchoPacket.csvForG2(secondList, echoName2, delay);
         EchoPacket.r1Diagram(elapsedList, r1Name, delay);
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         outSocket.close();
         inSocket.close();
      }
   }


   /**
    * Method for creating a CSV file.
    *
    * @param strList  List of strings to write to the CSV.
    * @param fileName The name of the CSV file to create.
    * @throws IOException If an I/O error occurs.
    */
   public static void toCsvFile(List<String> strList, String fileName) throws IOException {
      FileWriter csvWriter = new FileWriter(fileName);

      for (String data : strList) {
         csvWriter.append(String.join(",", data));
         csvWriter.append("\n");
      }

      csvWriter.flush();
      csvWriter.close();
   }


   /**
    * Method for creating a CSV for diagram G2.
    *
    * @param secondList List of seconds at which each packet was received.
    * @param fileName   The name of the CSV file to create.
    * @param delay      Whether to include delays in the statistics.
    * @throws IOException If an I/O error occurs.
    */
   public static void csvForG2(ArrayList<Long> secondList, String fileName, boolean delay) throws IOException {
      int packetCounter = 0, packetSum = 0;
      long second = 0, throughput, secondNum = 0; // Full second's stats
      List<String> strList = new ArrayList<String>();
      ArrayList<Integer> packetsPerSecond = new ArrayList<Integer>();

      secondNum = secondList.get(secondList.size() - 1) - secondList.get(0) + 1;
      second = secondList.get(0);
      packetCounter++;

      for (int i = 1; i < secondList.size(); i++) {
         if (second == secondList.get(i)) {
               packetCounter++;
         } else {
               while (second < secondList.get(i)) {
                  packetsPerSecond.add(packetCounter);
                  packetSum += packetCounter;
                  packetCounter = 0;
                  second++;
               }
               second = secondList.get(i);
               packetCounter = 1;
         }
      }

      if (delay) {
         while (packetsPerSecond.size() < secondNum) {
               packetsPerSecond.add(packetCounter);
               packetCounter = 0;
         }
      }

      for (int i = 0; i < packetsPerSecond.size(); i++) {
         System.out.println("Second: " + (i + 1) + " packets downloaded: " + packetsPerSecond.get(i));
      }

      packetSum = 0;
      for (int i = 0; i < packetsPerSecond.size(); i++) {
         if (i < 7) {
               for (int j = 0; j <= i; j++) {
                  packetSum += packetsPerSecond.get(j);
               }
               throughput = packetSum * 32 * 8 / (i + 1); // in bits per second
         } else {
               for (int j = 0; j < 8; j++) {
                  packetSum += packetsPerSecond.get(i - j);
               }
               throughput = packetSum * 32 * 8 / 8; // in bits per second
         }
         strList.add(String.valueOf(throughput));
         packetSum = 0;
      }
      EchoPacket.toCsvFile(strList, fileName);
   }


   /**
    * Method for creating a CSV for R1 diagram.
    *
    * @param elapsedList List of elapsed times for each packet.
    * @param name        The name of the CSV file to create.
    * @param delay       Whether to include delays in the statistics.
    * @throws IOException If an I/O error occurs.
    */
   public static void r1Diagram(ArrayList<Long> elapsedList, String name, boolean delay) throws IOException {
      int packetNum = elapsedList.size();
      int c = 4;
      float a = 0.875f, b = 0.75f;

      if (delay) {
         long[] srttArr = new long[packetNum];
         srttArr[0] = (long) ((1 - a) * elapsedList.get(0));

         long[] rttvArr = new long[packetNum]; // round trip time variance = σ
         rttvArr[0] = (long) ((1 - b) * Math.abs((srttArr[0] - elapsedList.get(0))));

         long[] rtoArr = new long[packetNum];
         rtoArr[0] = (long) (srttArr[0] + c * rttvArr[0]);

         for (int i = 1; i < packetNum; i++) {
               srttArr[i] = (long) (a * srttArr[i - 1] + (1 - a) * elapsedList.get(i));
               rttvArr[i] = (long) (b * rttvArr[i - 1] + (1 - b) * Math.abs((srttArr[i] - elapsedList.get(i))));
               rtoArr[i] = (long) (srttArr[i] + c * rttvArr[0]);
         }

         FileWriter csvWriter = new FileWriter(name);
         csvWriter.append(String.join(",", "RTT", "SRTT", "RTTV", "RTO"));
         csvWriter.append("\n");

         for (int j = 0; j < packetNum; j++) {
               System.out.println(srttArr[j] + "   " + rttvArr[j] + "   " + rtoArr[j]);
               csvWriter.append(String.join(",", String.valueOf(elapsedList.get(j)), String.valueOf(srttArr[j]),
                     String.valueOf(rttvArr[j]), String.valueOf(rtoArr[j])));
               csvWriter.append("\n");
         }

         csvWriter.flush();
         csvWriter.close();
      } else {
         float[] srttArr = new float[packetNum];
         srttArr[0] = ((1 - a) * elapsedList.get(0));

         float[] rttvArr = new float[packetNum]; // round trip time variance = σ
         rttvArr[0] = ((1 - b) * Math.abs((srttArr[0] - elapsedList.get(0))));

         float[] rtoArr = new float[packetNum];
         rtoArr[0] = (srttArr[0] + c * rttvArr[0]);

         for (int i = 1; i < packetNum; i++) {
               srttArr[i] = (a * srttArr[i - 1] + (1 - a) * elapsedList.get(i));
               rttvArr[i] = (b * rttvArr[i - 1] + (1 - b) * Math.abs((srttArr[i] - elapsedList.get(i))));
               rtoArr[i] = (srttArr[i] + c * rttvArr[0]);
         }

         FileWriter csvWriter = new FileWriter(name);

         for (int j = 0; j < packetNum; j++) {
               System.out.println(srttArr[j] + "   " + rttvArr[j] + "   " + rtoArr[j]);
               csvWriter.append(String.join(",", String.valueOf(elapsedList.get(j)), String.valueOf(srttArr[j]),
                     String.valueOf(rttvArr[j]), String.valueOf(rtoArr[j])));
               csvWriter.append("\n");
         }

         csvWriter.flush();
         csvWriter.close();
      }
   }

   
   /**
    * Method for sending requests and receiving 4 echo packets for Wireshark screenshots.
    *
    * @param serverPort The server's port for sending echo requests.
    * @param clientPort The client's port for receiving echo responses.
    * @throws IOException If an I/O error occurs.
    */
   public static void echo4Wireshark(int serverPort, int clientPort) throws IOException {
      String echoRequestCode = "E0000"; // Echo request code

      // Create a DatagramSocket for sending echo requests
      DatagramSocket outSocket = new DatagramSocket();

      // Create an empty buffer for receiving echo responses
      byte[] inBuffer = new byte[2048];

      // Define the server's IP address
      byte[] hostIp = {(byte) 155, (byte) 207, (byte) 18, (byte) 208};
      InetAddress hostAddress = InetAddress.getByAddress(hostIp);

      // Create a DatagramPacket for sending echo requests
      DatagramPacket outPacket = new DatagramPacket(echoRequestCode.getBytes(), echoRequestCode.length(), hostAddress, serverPort);

      // Create a DatagramSocket for receiving echo responses
      DatagramSocket inSocket = new DatagramSocket(clientPort);
      inSocket.setSoTimeout(8000); // Set a timeout for receiving responses

      // Loop to send echo requests and receive echo responses 4 times
      for (int i = 0; i < 4; i++) {
         // Send the echo request
         outSocket.send(outPacket);

         // Receive the echo response
         DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
         inSocket.receive(inPacket);

         // Convert the response to a string
         String message = new String(inPacket.getData(), 0, inPacket.getLength());

         // Print the received message
         System.out.println("Received: " + message);
      }

      // Close the sockets
      outSocket.close();
      inSocket.close();
   }
}
