import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Sender {
	private static final int SEQ_NUM = 41;
	public static void main(String[] args) throws Exception{
		if (args.length != 8){
			System.out.println("Required arguements: [Receiver_Host_IP] [Receiver_Port] [file.txt] [MWS] [MMS] [Timeout] [PDrop] [seed]\n");
			return;
		}
		/*args*/
		String host = args[0];
		InetAddress IP = InetAddress.getByName(host);
		int port = Integer.parseInt(args[1]);
		String inputFile = args[2];
		int MMS = Integer.parseInt(args[3]);
		/*----*/
		
		/*get file*/
		byte[] inputByte = Files.readAllBytes(Paths.get(inputFile));
		/* debugging
		String inputString = Arrays.toString(inputByte);
		*/
		
		byte[]cutByte = new byte[MMS];
		int bytePosition = 0;
		/*-------*/
		
		/*initialise socket*/
		DatagramSocket senderSocket = new DatagramSocket();
		/*-----------------*/
		
		/*cut Strings into MMS size and send*/
		while (bytePosition < inputByte.length){
			
			/*initialise send and receive array*/
			byte[] sendData = new byte[1024];
			byte[] receiveData = new byte[1024];
			/*------------------------------*/
			
			//initialised string cut to MMS
			String cutString;
			//position
			int i = 0;
			
			/*the cutter*/
			while (i < cutByte.length && bytePosition < inputByte.length){
				cutByte[i] = inputByte[bytePosition];
				i++;
				bytePosition++;
			}
			i = 0;
			/*----------*/
			
			cutString = new String(cutByte);
			
			//add to packet
			//TODO need to properly figure out seq numbering
			String[] sendStringA = packet(false,false,false,SEQ_NUM,0,cutString);
			//conversion
			sendData = StringAToByteA(sendStringA);
			//sends packet
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IP, port);
			senderSocket.send(sendPacket);
			System.out.println("Sent");
			
			//wait for ack
		}
		senderSocket.close();
	}
	
	// Packet Maker //
	public static String[] packet (boolean SYN, boolean ACK, boolean FIN, int SeqNo, int AckNo, String partialString){
		String[] pack = new String[6];
		
		pack[0] = Boolean.toString(SYN);
		pack[1] = Boolean.toString(ACK);
		pack[2] = Boolean.toString(FIN);
		pack[3] = Integer.toString(SeqNo);
		pack[4] = Integer.toString(AckNo);
		pack[5] = partialString;
		
		return pack;
	}
	
	// Converters //
	public static byte[] StringAToByteA (String[] source) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(source);
		oos.flush();
		oos.close();
		byte[] converted = baos.toByteArray();
		return converted;
	}
	
	public static String[] ByteAToStringA (byte[] source) throws IOException, ClassNotFoundException{
		ByteArrayInputStream bais = new ByteArrayInputStream(source);
		ObjectInputStream ois = new ObjectInputStream(bais);
		String[] converted = (String[]) ois.readObject();
		ois.close();
		return converted;
	}
}
