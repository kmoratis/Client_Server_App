import java.io.*;
import java.net.*;
import java.util.Scanner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class Image {
   
   public static void imageMenu(int serverPort, int clientPort, String imageRequest, Scanner scanner) {
      try {
         InetAddress serverAddress = InetAddress.getByName("server_address_here"); // Replace with the actual server address

         DatagramSocket clientSocket = new DatagramSocket(clientPort);

         while (true) {
               // Get user input
               System.out.print("Press Enter to request an image (or type 'exit' to exit): ");
               String userInput = scanner.nextLine();

               if ("exit".equalsIgnoreCase(userInput)) {
                  break;
               }

               // Send the image request to the server
               byte[] sendData = imageRequest.getBytes();
               DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
               clientSocket.send(sendPacket);

               // Receive the image data from the server
               byte[] receiveData = new byte[1024]; // Adjust buffer size as needed
               DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
               clientSocket.receive(receivePacket);

               // Process and display the received image data
               ByteArrayInputStream imageStream = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
               BufferedImage receivedImage = ImageIO.read(imageStream);

               // You can display or save the image as needed
               // For example:
               // ImageIO.write(receivedImage, "jpg", new File("received_image.jpg"));
               // Display the image using a GUI library or framework

               System.out.println("Image Received!");
         }

         clientSocket.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }


   /**
    * Downloads and saves an image from a remote server.
    *
    * @param serverPort       The server's port for sending image requests.
    * @param clientPort       The client's port for receiving image data.
    * @param imageRequestCode The image request code.
    * @param camNum           The camera number (1 or 2).
    * @throws IOException If an I/O error occurs.
    */
   public static void image(int serverPort, int clientPort, String imageRequestCode, int camNum)
         throws IOException {
      // Append optional parameters to the image request code
      imageRequestCode += "FLOW=ON"; // Enable image flow
      if (camNum == 2) {
         imageRequestCode += "CAM=PTZ"; // Specify camera number 2 (CAM=PTZ)
      }

      // Define the server's IP address
      InetAddress hostAddress = InetAddress.getByName("155.207.18.208");

      // Create DatagramSockets
      try (DatagramSocket outSocket = new DatagramSocket();
            DatagramSocket inSocket = new DatagramSocket(clientPort)) {

         inSocket.setSoTimeout(8000); // Set a timeout for receiving packets

         // Create buffers for sending image request and next message
         byte[] outBuffer = imageRequestCode.getBytes();
         byte[] nextBuffer = "NEXT".getBytes();

         // Create a file to save the received image
         File myFile = new File("C:\\Users\\kwsta\\Desktop\\userProject\\session1files\\image2.jpeg");
         try (FileOutputStream imageStream = new FileOutputStream(myFile)) {
               System.out.println("Downloading image...");

               // Send the initial image request
               DatagramPacket outPacket = new DatagramPacket(outBuffer, outBuffer.length, hostAddress, serverPort);
               outSocket.send(outPacket);

               while (true) {
                  // Receive a packet
                  byte[] inBuffer = new byte[128];
                  DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                  inSocket.receive(inPacket);

                  // Search for the end of the image in the packet (last packet)
                  boolean isLastPacket = false;
                  for (int i = 0; i < inPacket.getLength() - 1; i++) {
                     if (inPacket.getData()[i] == (byte) 0xFF && inPacket.getData()[i + 1] == (byte) 0xD9) {
                           isLastPacket = true;
                           break;
                     }
                  }

                  if (isLastPacket) {
                     System.out.println("Last packet received");
                     imageStream.write(inPacket.getData(), 0, inPacket.getLength());
                     break;
                  }

                  System.out.println("Packet received");
                  imageStream.write(inPacket.getData(), 0, inPacket.getLength());

                  // Send the "NEXT" message to request the next packet
                  DatagramPacket nextPacket = new DatagramPacket(nextBuffer, nextBuffer.length, hostAddress, serverPort);
                  outSocket.send(nextPacket);
               }

               System.out.println("Image downloaded successfully and saved as 'image2.jpeg'");
         }
      }
   }
}
