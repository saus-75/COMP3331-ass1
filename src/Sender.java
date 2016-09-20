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
		Random rand = new Random(seed);
		PrintWriter log = new PrintWriter("Sender_log.txt");
		long start = System.currentTimeMillis();
		long relative = 0;
		
		//Log vars
		long totalData = 0;
		int sent = 0;
		int drops = 0;
		int retrans = 0;
		
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
			relative = System.currentTimeMillis() - start;
			log.println("snd" + "  " + relative + " S    " + 0 +" "+SN);
			System.out.println("SYN sent");
			sent++;
			
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
					relative = System.currentTimeMillis() - start;
					log.println("rcv" + "  " + relative + " SA   " + 0 +" "+SN);
					String[]  ACKString = packet(false,true,false,SN,ACKno+1,null);
					byte[] ACK = StringAToByteA(ACKString);
					
					DatagramPacket ACKPacket = new DatagramPacket(ACK, ACK.length, IP, port);
					senderSocket.send(ACKPacket);
					System.out.println("ACK sent");
					sent++;
					relative = System.currentTimeMillis() - start;
					log.println("snd" + "  " + relative + " A    " + 0 +" "+ SN);
					System.out.println("Connection Established");
					connected = 1;
				}
			} catch (SocketTimeoutException e){
				//or else close socket and exit
				System.err.println("Connection Not Established");
				log.close();
				senderSocket.close();
				return;
			}
		}
		/*---------------*/
		
		/*cut Strings into MMS size and send*/
		while (bytePosition < inputByte.length && connected == 1){
			
			float pld = rand.nextFloat();
			System.out.println("pld is: " + pld);
			
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
			if (pld <= PDrop){
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IP, port);
				senderSocket.send(sendPacket);
				System.out.println("Sent");
				sent++;
				totalData += cutString.getBytes().length;
				relative = System.currentTimeMillis() - start;
				log.println("snd" + "  " + relative + " D    " + cutString.getBytes().length +" "+SN);
			} else {
				System.out.println("Packet Dropped");
				relative = System.currentTimeMillis() - start;
				log.println("drop" + " " + relative + " D    " + cutString.getBytes().length +" "+SN);
				drops++;
				totalData += cutString.getBytes().length;
			}
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
					relative = System.currentTimeMillis() - start;
					log.println("rcv" + "  " + relative + " A    " + 0 +" "+ SN);
					pastPos = bytePosition;
				}
			} catch (SocketTimeoutException e){
				System.err.println("Packet Lost");
				bytePosition = pastPos;
				SN -= dataSize;
				System.out.println("Retransmitting");
				retrans++;
			}
			/*-------------*/
		}
		
		//4-way connection teardown
		System.out.println("Connection teardown initatied...");
		String[] FIN1S = packet(false,false,true,SN,ACKno,null);
		byte[] FIN1 = StringAToByteA(FIN1S);
		//FIN
		DatagramPacket FINPacket1 = new DatagramPacket(FIN1, FIN1.length, IP, port);
		senderSocket.send(FINPacket1);
		System.out.println("FIN Sent");
		sent++;
		relative = System.currentTimeMillis() - start;
		log.println("rcv" + "  " + relative + " F    " + 0 +" "+ SN);
		SN++;
		
		//FINACK
		DatagramPacket FINACKPacket = new DatagramPacket(new byte[1024], 1024);
		senderSocket.receive(FINACKPacket);
		System.out.println("FINACK received");
		relative = System.currentTimeMillis() - start;
		log.println("snd" + "  " + relative + " FA   " + 0 +" "+ SN);
		byte[] FINACKB = FINACKPacket.getData();
		String[] FINACK = ByteAToStringA(FINACKB);
		//ACK and close
		if (FINACK[1].equals("true") && FINACK[2].equals("true")){	
			String[] ACKS = packet(false,true,false,SN,ACKno,null);
			System.out.println(ACKS[1]);
			byte[] ACKB = StringAToByteA(ACKS);
			DatagramPacket ACKPacket = new DatagramPacket(ACKB, ACKB.length, IP, port);
			senderSocket.send(ACKPacket);
			System.out.println("ACK Sent");
			sent++;
			relative = System.currentTimeMillis() - start;
			log.println("snd" + "  " + relative + " A    " + 0 +" "+ SN);
			SN++;
			System.out.println("Socket closed");
			log.println("Data Transferred " + totalData + "\nSegment Sent " + sent + "\nPackets Dropped " + drops + "\nRestransmitted Packet " + retrans);
			log.close();
			senderSocket.close();
			return;
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
