import java.io.*;
import java.net.*;

public class Receiver {
	private static final int SEQ_NUM = 911;
	public static void main(String[] args) throws Exception{
		if (args.length != 2){
			System.out.println("Required arguements: [Receiver_Port] [file.txt]\n");
			return;
		}
		int port = Integer.parseInt(args[0]);
		String outputFile = args[1];
		
		DatagramSocket receiver = new DatagramSocket(port);
		
		String whole = "";
		int connected = 0;
		InetAddress clientHost = null;
		int clientPort = 0;
		int SN = SEQ_NUM;
		int senderACKno = 0;
		
		//3-way handshake
		while (connected != 1){
			DatagramPacket SYNPacket = new DatagramPacket (new byte[1024], 1024);
			receiver.receive(SYNPacket);
			byte[] SYNData = SYNPacket.getData();
			String[] SYN = ByteAToStringA(SYNData);
			
			System.out.println("SYN received");
			
			clientHost = SYNPacket.getAddress();
			clientPort = SYNPacket.getPort();
			
			senderACKno = Integer.parseInt(SYN[3]);
			
			if (SYN[0].equals("true")){
				String[] SYNACK = heading(true, true, false, SN, senderACKno++);
				byte[] SYNACKBuf = StringAToByteA(SYNACK);
				DatagramPacket SYNACKPacket = new DatagramPacket(SYNACKBuf, SYNACKBuf.length, clientHost, clientPort);
				receiver.send(SYNACKPacket);
				System.out.println("SYNACK sent");
				
				DatagramPacket ACKPacket = new DatagramPacket (new byte[1024], 1024);
				receiver.receive(ACKPacket);
				System.out.println("ACK received");
				byte[] ACKData = ACKPacket.getData();
				String[] ACK = ByteAToStringA(ACKData);
				if (ACK[1].equals("true")){
					SN = Integer.parseInt(ACK[4]);
					connected = 1;
					System.out.println("Connection Established");
				} else {
					System.out.println("Connection Not Established");
				}
			}
			
			
		}
		while (connected == 1) {
			DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
			//receive
			receiver.receive(request);
			System.out.println("Packet Received");
			byte[] byteData = request.getData();
			String[] stringData = ByteAToStringA(byteData);
			
			if (stringData[2].equals("false")){
				//String concat
				String back = stringData[5];
				whole += back;
				//System.out.println(whole + " {" + senderSeqNo + "}");
				senderACKno += back.getBytes().length;
				//reply
				String[] ACK = heading(false, true, false, SEQ_NUM, senderACKno);
				byte[] replyBuf = StringAToByteA(ACK);
				DatagramPacket reply = new DatagramPacket(replyBuf, replyBuf.length, clientHost, clientPort);
				receiver.send(reply);
				
				System.out.println("Reply Sent!");
				
			//4-way connection teardown
				//First FIN
			}else if (stringData[2].equals("true")){
				System.out.println("Connection teardown initiated...");
				
				//First ACK
				String[] ACK1 = heading(true, true, false, SN, senderACKno++);
				byte[] ACK1Buf = StringAToByteA(ACK1);
				DatagramPacket ACK1Packet = new DatagramPacket(ACK1Buf, ACK1Buf.length, clientHost, clientPort);
				receiver.send(ACK1Packet);
				System.out.println("ACK sent");
				
				//Second FIN
				DatagramPacket FIN2Packet = new DatagramPacket(new byte[1024], 1024);
				receiver.receive(FIN2Packet);
				byte[] FIN2B = request.getData();
				String[] FIN2 = ByteAToStringA(FIN2B);
				
				if (FIN2[2].equals("true")){
					//Second ACK
					String[] ACK2 = heading(true, true, false, SN, senderACKno++);
					byte[] ACK2Buf = StringAToByteA(ACK2);
					DatagramPacket ACK2Packet = new DatagramPacket(ACK2Buf, ACK2Buf.length, clientHost, clientPort);
					receiver.send(ACK2Packet);
					System.out.println("ACK sent");
					receiver.close();
					//output string
					String trimmer = whole.trim();
					outputText(trimmer, outputFile);
				}
			}
		}
	}
	
	//server header maker 
	public static String[] heading (boolean SYN, boolean ACK, boolean FIN, int SeqNo, int AckNo){
		String[] head = new String[5];
		
		head[0] = Boolean.toString(SYN);
		head[1] = Boolean.toString(ACK);
		head[2] = Boolean.toString(FIN);
		head[3] = Integer.toString(SeqNo);
		head[4] = Integer.toString(AckNo);
		
		return head;
	}
	
	//basic header printer
	public static void printData(String[] data){
		int i = 0;
		while (i < data.length){
			System.out.println(data[0]);
		}
	}
	
	//text output
	public static void outputText(String text, String output) throws FileNotFoundException{
		PrintWriter out = new PrintWriter(output);
		out.println(text);
		out.close();
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
