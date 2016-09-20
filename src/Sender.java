import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

public class Sender {
	private static final int SEQ_NUM = 420;
	public static void main(String[] args) throws Exception{
		if (args.length != 8){
			System.out.println("Required arguements: [Receiver_Host_IP] [Receiver_Port] [file.txt] [MWS] [MMS] [Timeout] [PDrop] [seed]\n");
			return;
		}
		//args
		InetAddress IP = InetAddress.getByName(args[0]);
		int port = Integer.parseInt(args[1]);
		String inputFile = args[2];
		int MWS = Integer.parseInt(args[3]);
		int MMS = Integer.parseInt(args[4]);
		int timeout = Integer.parseInt(args[5]);
		float PDrop = Float.parseFloat(args[6]);
		int seed = Integer.parseInt(args[7]);
		
		//get file and other initialisation
		byte[] inputByte = Files.readAllBytes(Paths.get(inputFile));
		int SN = SEQ_NUM;
		int connected = 0;
		byte[]cutByte = new byte[MMS];
		int bytePosition = 0;
		int pastPos = 0;
		int ACKno = 0;
		Random pld = new Random(seed);
				
		//initialise socket
		DatagramSocket senderSocket = new DatagramSocket();
		
		//initialise send and receive array
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		
		/*3 Way handshake*/
		if (connected != 1){
			
			String[]  SYNString = packet(true,false,false,SN,ACKno,null);
			byte[] SYN = StringAToByteA(SYNString);
			
			//Send SYN
			DatagramPacket SYNPacket = new DatagramPacket(SYN, SYN.length, IP, port);
			senderSocket.send(SYNPacket);
			System.out.println("SYN sent");
			
			//Wait for SYNACK reply
			DatagramPacket SYNACKPacket = new DatagramPacket(new byte[1024], 1024);
			senderSocket.setSoTimeout(timeout);
			
			try{
				senderSocket.receive(SYNACKPacket);
				byte[] SYNACKDataByte = SYNACKPacket.getData();
				String[] SYNACK = ByteAToStringA(SYNACKDataByte);
				System.out.println("SYNACK received");
				if (SYNACK[0].equals("true") && SYNACK[1].equals("true")){
					//Reply with an ACK
					ACKno = Integer.parseInt(SYNACK[3]);
					SN++;
					
					String[]  ACKString = packet(false,true,false,SN,ACKno+1,null);
					byte[] ACK = StringAToByteA(ACKString);
					
					DatagramPacket ACKPacket = new DatagramPacket(ACK, ACK.length, IP, port);
					senderSocket.send(ACKPacket);
					System.out.println("ACK sent");
					
					System.out.println("Connection Established");
					connected = 1;
				}
			} catch (SocketTimeoutException e){
				//or else close socket and exit
				System.err.println("Connection Not Established");
				senderSocket.close();
				return;
			}
		}
		/*---------------*/
		
		/*cut Strings into MMS size and send*/
		while (bytePosition < inputByte.length && connected == 1){
			
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
			/*----------*/
			
			cutString = new String(cutByte);
			
			//add to packet
			//TODO need to properly figure out seq numbering
			String[] sendStringA = packet(false,false,false,SN,ACKno,cutString);
			
			//conversion
			sendData = StringAToByteA(sendStringA);
			
			//sends packet
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IP, port);
			senderSocket.send(sendPacket);
			System.out.println("Sent");
			
			//Sanitising my int and my Byte arrays
			i = 0;
			int dataSize = cutByte.length;
			cutByte = new byte[cutByte.length];
			
			/*wait for ack*/
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			//Set timeout
			senderSocket.setSoTimeout(timeout);
			try {
				senderSocket.receive(receivePacket);
				receiveData = receivePacket.getData();
				String[] received = ByteAToStringA(receiveData);
				if (received[1].equals("true")){
					SN += dataSize;
					System.out.println("ACK received");
					pastPos = bytePosition;
				}
			} catch (SocketTimeoutException e){
				System.err.println("Packet Lost");
				bytePosition = pastPos;
				System.out.println("Retransmitting");
			}
			/*-------------*/
		}
		
		//4-way connection teardown
		System.out.println("Connection teardown initatied...");
		String[] FIN1S = packet(false,false,true,SN,ACKno,null);
		byte[] FIN1 = StringAToByteA(FIN1S);
		//First FIN
		DatagramPacket sendPacket1 = new DatagramPacket(FIN1, FIN1.length, IP, port);
		senderSocket.send(sendPacket1);
		System.out.println("FIN 1 Sent");
		SN++;
		
		DatagramPacket ACK1Packet = new DatagramPacket(new byte[1024], 1024);
		senderSocket.receive(ACK1Packet);
		System.out.println("ACK received");
		byte[] ACK1B = ACK1Packet.getData();
		String[] ACK1 = ByteAToStringA(ACK1B);
		
		if (ACK1[1].equals("true")){
			String[] FIN2S = packet(false,false,true,SN,ACKno,null);
			byte[] FIN2 = StringAToByteA(FIN2S);
			//Second FIN
			DatagramPacket sendPacket2 = new DatagramPacket(FIN2, FIN2.length, IP, port);
			senderSocket.send(sendPacket2);
			System.out.println("FIN 2 Sent");
			SN++;
			
			DatagramPacket ACK2Packet = new DatagramPacket(new byte[1024], 1024);
			senderSocket.receive(ACK2Packet);
			System.out.println("ACK received");
			byte[] ACK2B = ACK2Packet.getData();
			String[] ACK2 = ByteAToStringA(ACK2B);
			
			if (ACK2[1].equals("true")){
				System.out.println("Socket closed");
				senderSocket.close();
			}
		}
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
