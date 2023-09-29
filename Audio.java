import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Audio {

   public static void audioMenu(int serverPort, int clientPort, String audioRequest, Scanner scanner) {
      try {
         InetAddress serverAddress = InetAddress.getByName("server_address_here"); // Replace with the actual server address

         DatagramSocket clientSocket = new DatagramSocket(clientPort);

         // Define audio format for playback
         AudioFormat audioFormat = new AudioFormat(8000.0f, 16, 1, true, false);

         while (true) {
               // Get user input
               System.out.print("Press Enter to request audio (or type 'exit' to exit): ");
               String userInput = scanner.nextLine();

               if ("exit".equalsIgnoreCase(userInput)) {
                  break;
               }

               // Send the audio request to the server
               byte[] sendData = audioRequest.getBytes();
               DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
               clientSocket.send(sendPacket);

               // Receive audio data from the server
               byte[] receiveData = new byte[1024]; // Adjust buffer size as needed
               DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
               clientSocket.receive(receivePacket);

               // Create an audio input stream from the received data
               ByteArrayInputStream audioStream = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
               AudioInputStream audioInputStream = new AudioInputStream(audioStream, audioFormat, receivePacket.getLength());

               // Play the audio using a SourceDataLine
               SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
               sourceDataLine.open(audioFormat);
               sourceDataLine.start();

               byte[] audioBuffer = new byte[1024];
               int bytesRead;

               while ((bytesRead = audioInputStream.read(audioBuffer, 0, audioBuffer.length)) != -1) {
                  sourceDataLine.write(audioBuffer, 0, bytesRead);
               }

               sourceDataLine.drain();
               sourceDataLine.close();

               System.out.println("Audio Received and Played!");
         }

         clientSocket.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }


   /**
    * Method for creating audio requests.
    *
    * @param serverPort      The server's port for sending audio requests.
    * @param clientPort      The client's port for receiving audio responses.
    * @param audioRequestCode The audio request code.
    * @param soundSource     The sound source identifier.
    * @param adaptive        Whether to use adaptive encoding.
    * @throws IOException If an I/O error occurs.
    * @throws LineUnavailableException If there is an issue with audio playback.
    */
   public static void audio(int serverPort, int clientPort, String audioRequestCode, String soundSource, boolean adaptive)
         throws IOException, LineUnavailableException {

      String aq = "AQ";
      String dpcmRequest = audioRequestCode + soundSource;
      String aqRequest = audioRequestCode + aq + soundSource;
      String wavPath = "C:\\Users\\kwsta\\Desktop\\userProject\\session1files\\javaAudioG10.wav"; // for G9 and G10
      String wavAqPath = "C:\\Users\\kwsta\\Desktop\\userProject\\session1files\\javaAudioAQ.wav";
      String dpcmDiff = "dpcmDiffG11.csv"; // for G11
      String dpcmSamp = "dpcmSampG12.csv"; // for G12
      String aqDiff = "aqDiffG13Useless.csv"; // G13
      String aqSamp = "aqSampG14Useless.csv"; // G14
      String aqMean = "aqMean17.csv"; // G15 and G17
      String aqStep = "aqStep18.csv"; // G16 and G18

      if (!adaptive) { // DPCM non-adaptive
         ArrayList<byte[]> dpcmList = new ArrayList<byte[]>(Audio.download(serverPort, clientPort, dpcmRequest, 128));
         ArrayList<Byte> decodedDPCM = new ArrayList<Byte>(Audio.decodeDPCM(dpcmList, dpcmDiff, dpcmSamp));
         Audio.play(decodedDPCM, 8, wavPath);
      } else {
         ArrayList<byte[]> aqList = new ArrayList<byte[]>(Audio.download(serverPort, clientPort, aqRequest, 132));
         ArrayList<Byte> decodedAq = new ArrayList<Byte>(Audio.decodeAQ(aqList, aqDiff, aqSamp, aqMean, aqStep));
         Audio.play(decodedAq, 16, wavAqPath);
      }
   }


   /**
    * Method for downloading audio clips (both DPCM and AQDPCM) and frequencies.
    *
    * @param serverPort The server's port for sending audio requests.
    * @param clientPort The client's port for receiving audio responses.
    * @param request    The audio request code.
    * @param bufferSize The size of the buffer for receiving data.
    * @return A list of downloaded audio packets.
    * @throws IOException If an I/O error occurs.
    */
   public static ArrayList<byte[]> download(int serverPort, int clientPort, String request, int bufferSize)
         throws IOException {

      byte[] hostIp = {(byte) 155, (byte) 207, (byte) 18, (byte) 208};
      InetAddress hostAddress = InetAddress.getByAddress(hostIp);
      DatagramSocket outSocket = new DatagramSocket();
      byte[] outBuffer = request.getBytes();
      DatagramPacket outPacket = new DatagramPacket(outBuffer, outBuffer.length, hostAddress, serverPort);

      DatagramSocket inSocket = new DatagramSocket(clientPort);
      inSocket.setSoTimeout(10000);
      byte[] inBuffer = new byte[bufferSize];
      DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);

      ArrayList<byte[]> packetList = new ArrayList<byte[]>();

      outSocket.send(outPacket);
      System.out.println("Audio Request Sent");

      for (int i = 0; i < 900; i++) {
         inSocket.receive(inPacket);
         packetList.add(inPacket.getData().clone());
         System.out.println("Packet " + (i + 1) + " downloaded");
      }

      outSocket.close();
      inSocket.close();

      return packetList;
   }


   /**
    * Method for decoding DPCM audio data and saving the samples to a .wav file.
    *
    * @param packetList List of audio packets to decode.
    * @param dpcmDiff   Path to save DPCM differences.
    * @param dpcmSamp   Path to save DPCM samples.
    * @return List of decoded audio samples.
    * @throws IOException If an I/O error occurs.
    */
   public static ArrayList<Byte> decodeDPCM(ArrayList<byte[]> packetList, String dpcmDiff, String dpcmSamp)
         throws IOException {
            int nibble1 = 0, nibble2 = 0, step = 1, mean = 0; // step = b, mean = μ
            int maxValue = 127, minValue = -128;
            int diff1 = 0, diff2 = 0, sample1 = 0, sample2 = 0, prevSample = 0;
            ArrayList<Byte> samples = new ArrayList<Byte>();
      
            FileWriter diffWriter = new FileWriter(dpcmDiff);
            FileWriter sampWriter = new FileWriter(dpcmSamp);
      
            diffWriter.append(String.join(",", "Differences"));
            diffWriter.append("\n");
            sampWriter.append(String.join(",", "Samples"));
            sampWriter.append("\n");
      
            System.out.println("Decoding DPCM...");
      
            for (byte[] packet : packetList) {
                  prevSample = 0;
      
                  for (int i = 0; i < 128; i++) {
                     nibble1 = (packet[i] & 15); // 15 = 00001111
                     nibble2 = ((packet[i] & 240) >> 4); // 240 = 11110000
      
                     diff1 = (nibble1 - 8) * step + mean;
                     diff2 = (nibble2 - 8) * step + mean;
      
                     sample1 = diff1 + prevSample;
      
                     if (sample1 > maxValue) {
                        sample1 = maxValue;
                     } else if (sample1 < minValue) {
                        sample1 = minValue;
                     }
      
                     sample2 = diff2 + sample1;
      
                     if (sample2 > maxValue) {
                        sample2 = maxValue;
                     } else if (sample2 < minValue) {
                        sample2 = minValue;
                     }
      
                     prevSample = sample2;
      
                     samples.add((byte) sample1);
                     samples.add((byte) sample2);
      
                     diffWriter.append(String.join(",", String.valueOf(diff1)));
                     diffWriter.append("\n");
                     diffWriter.append(String.join(",", String.valueOf(diff2)));
                     diffWriter.append("\n");
      
                     sampWriter.append(String.join(",", String.valueOf(sample1)));
                     sampWriter.append("\n");
                     sampWriter.append(String.join(",", String.valueOf(sample2)));
                     sampWriter.append("\n");
                  }
            }
      
            diffWriter.flush();
            diffWriter.close();
            sampWriter.flush();
            sampWriter.close();
      
            return samples;
   }


   /**
    * Method for playing audio samples in real-time and saving them to a .wav file.
    *
    * @param samples   List of audio samples to play.
    * @param q         Audio quality (e.g., 8 or 16 bits).
    * @param wavPath   Path to save the .wav file.
    * @throws LineUnavailableException If there is an issue with audio playback.
    * @throws IOException             If an I/O error occurs.
    */
   public static void play(ArrayList<Byte> samples, int q, String wavPath)
         throws LineUnavailableException, IOException {
            byte[] samplesArray = new byte[samples.size()];

            for (int j = 0; j < samples.size(); j++) {
                samplesArray[j] = samples.get(j);
            }
    
            try (FileOutputStream fos = new FileOutputStream(wavPath)) {
                fos.write(samplesArray);
            }
    
            AudioFormat auFormat = new AudioFormat(8000, q, 1, true, false);
            SourceDataLine dataLine = AudioSystem.getSourceDataLine(auFormat);
    
            dataLine.open(auFormat, 32000);
            dataLine.start();
            dataLine.write(samplesArray, 0, samples.size());
    
            dataLine.drain();
            dataLine.stop();
   }


   /**
    * Method for decoding AQDPCM audio data and saving the samples to a .wav file.
    *
    * @param packetList List of audio packets to decode.
    * @param aqDiff     Path to save AQDPCM differences.
    * @param aqSamp     Path to save AQDPCM samples.
    * @param aqMean     Path to save AQDPCM mean values.
    * @param aqStep     Path to save AQDPCM step values.
    * @return List of decoded audio samples.
    * @throws IOException If an I/O error occurs.
    */
    public static ArrayList<Byte> decodeAQ(ArrayList<byte[]> packetList, String aqDiff, String aqSamp, String aqMean, String aqStep)
         throws IOException {
            int nibble1 = 0, nibble2 = 0, step = 0, mean = 0;
            int diff1 = 0, diff2 = 0, sample1 = 0, sample2 = 0, helpSample = 0;
            int maxValue = 32767, minValue = -32768;

            ArrayList<Byte> samples = new ArrayList<Byte>();
            FileWriter diffWriter = new FileWriter(aqDiff);
            FileWriter sampWriter = new FileWriter(aqSamp);
            FileWriter meanWriter = new FileWriter(aqMean);
            FileWriter stepWriter = new FileWriter(aqStep);

            diffWriter.append(String.join(",", "Differences"));
            diffWriter.append("\n");
            sampWriter.append(String.join(",", "Samples"));
            sampWriter.append("\n");

            System.out.println("-----------Decoding----------");

            for (byte[] packet : packetList) {
               mean = (packet[0] & 0xFF) << 8 | (packet[1] & 0xFF);
               step = (packet[2] & 0xFF) << 8 | (packet[3] & 0xFF);

               for (int i = 4; i < 132; i++) {
                  nibble1 = packet[i] & 0x0F;
                  nibble2 = (packet[i] & 0xF0) >> 4;

                  diff1 = (nibble1 - 8) * step + mean;
                  diff2 = (nibble2 - 8) * step + mean;

                  sample1 = diff1;
                  sample2 = diff2;

                  helpSample = ((sample1 & 0xFF00) >> 8);

                  if (helpSample > maxValue) {
                        helpSample = maxValue;
                  } else if (helpSample < minValue) {
                        helpSample = minValue;
                  }

                  samples.add((byte) helpSample);

                  helpSample = sample1 & 0xFF;

                  if (helpSample > maxValue) {
                        helpSample = maxValue;
                  } else if (helpSample < minValue) {
                        helpSample = minValue;
                  }

                  samples.add((byte) helpSample);

                  helpSample = ((sample2 & 0xFF00) >> 8);

                  if (helpSample > maxValue) {
                        helpSample = maxValue;
                  } else if (helpSample < minValue) {
                        helpSample = minValue;
                  }

                  samples.add((byte) helpSample);

                  helpSample = sample2 & 0xFF;

                  if (helpSample > maxValue) {
                        helpSample = maxValue;
                  } else if (helpSample < minValue) {
                        helpSample = minValue;
                  }

                  samples.add((byte) helpSample);

                  diffWriter.append(String.join(",", String.valueOf(diff1)));
                  diffWriter.append("\n");
                  diffWriter.append(String.join(",", String.valueOf(diff2)));
                  diffWriter.append("\n");

                  sampWriter.append(String.join(",", String.valueOf(sample1)));
                  sampWriter.append("\n");
                  sampWriter.append(String.join(",", String.valueOf(sample2)));
                  sampWriter.append("\n");

                  System.out.println("Diff1= " + diff1 + ", diff2= " + diff2 + ", sample1= " + sample1 + ", sample2=" + sample2);
               }

               System.out.println("mean= " + mean + ", step= " + step);
               meanWriter.append(String.join(",", String.valueOf(mean)));
               meanWriter.append("\n");
               stepWriter.append(String.join(",", String.valueOf(step)));
               stepWriter.append("\n");
            }

            diffWriter.flush();
            diffWriter.close();
            sampWriter.flush();
            sampWriter.close();
            stepWriter.flush();
            stepWriter.close();
            meanWriter.flush();
            meanWriter.close();

            return samples;
            }

            
   // Main method for testing
   public static void main(String[] args) {
      try {
         // Example usage:
         audio(1234, 5678, "AR001", "Sound1", true);
      } catch (IOException | LineUnavailableException e) {
         e.printStackTrace();
      }
   }
}
