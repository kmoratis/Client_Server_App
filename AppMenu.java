import java.util.Scanner;

/**
 * The main class for the user application that provides a menu for different tasks.
 */
public class AppMenu {

   public static void main(String[] args) throws Exception {

      // Initialize session information
      String date = "01/12/2020", sessionStart = "11:44", sessionEnd = "13:44";
      int serverPort = 38024, clientPort = 48024;
      String echoRequest = "E5675", imageRequest = "M3974", audioRequest = "A2138";
      int obdPacketNum = 178;

      try (Scanner scanner = new Scanner(System.in)) {
         // Display application header
         System.out.println("-------User Application-------");
         System.out.println("Session: " + sessionStart + " - " + sessionEnd + "  " + date);

         int userChoice;
         do {
               // Display menu options
               System.out.println("\nSelect an option:");
               System.out.println("1. Echo Packets");
               System.out.println("2. Image");
               System.out.println("3. Audio");
               System.out.println("4. Ithaki Copter Telemetry");
               System.out.println("5. Vehicle OBD");
               System.out.println("6. Server Info");
               System.out.println("0. Exit");
               userChoice = scanner.nextInt();

               switch (userChoice) {
                  case 1:
                     EchoPacket.echoMenu(serverPort, clientPort, echoRequest, scanner);
                     break;

                  case 2:
                     Image.imageMenu(serverPort, clientPort, imageRequest, scanner);
                     break;

                  case 3:
                     Audio.audioMenu(serverPort, clientPort, audioRequest, scanner);
                     break;

                  case 4:
                     IthakiCopter.telemetryMenu(serverPort, clientPort);
                     break;

                  case 5:
                     Vehicle.dataMenu(scanner, obdPacketNum);
                     break;

                  case 6:
                     ServerInfo.infoMenu(serverPort, clientPort);
                     break;

                  case 0:
                     System.out.println("Exiting the application. Goodbye!");
                     break;

                  default:
                     System.out.println("Invalid input. Please try again.");
               }
         } while (userChoice != 0);
      }
   }
}
