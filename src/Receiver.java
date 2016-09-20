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
		PrintWriter log = new PrintWriter("Receiver_log.txt");
		long start = System.currentTimeMillis();
		long relative = 0;
		
		//log vars
		long data = 0;
		int recv = 0;
	
		//3-way handshake
		while (connected != 1){
			DatagramPacket SYNPacket = new DatagramPacket (new byte[1024], 1024);
			receiver.receive(SYNPacket);
			byte[] SYNData = SYNPacket.getData();
			String[] SYN = ByteAToStringA(SYNData);
			
			System.out.println("SYN received");
			recv++;
			relative = System.currentTimeMillis() - start;
			log.println("rcv" + "  " + relative + " S    " + 0 +" "+SN);
			clientHost = SYNPacket.getAddress();
			clientPort = SYNPacket.getPort();
			
			senderACKno = Integer.parseInt(SYN[3]);
			
			if (SYN[0].equals("true")){
				String[] SYNACK = heading(true, true, false, SN, senderACKno++);
				byte[] SYNACKBuf = StringAToByteA(SYNACK);
				DatagramPacket SYNACKPacket = new DatagramPacket(SYNACKBuf, SYNACKBuf.length, clientHost, clientPort);
				receiver.send(SYNACKPacket);
				System.out.println("SYNACK sent");
				relative = System.currentTimeMillis() - start;
				log.println("snd" + "  " + relative + " SA   " + 0 +" "+SN);
				
				DatagramPacket ACKPacket = new DatagramPacket (new byte[1024], 1024);
				receiver.receive(ACKPacket);
				System.out.println("ACK received");
				recv++;
				relative = System.currentTimeMillis() - start;
				log.println("rcv" + "  " + relative + " A    " + 0 +" "+SN);
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
			recv++;
			byte[] byteData = request.getData();
			String[] stringData = ByteAToStringA(byteData);
			relative = System.currentTimeMillis() - start;
			log.println("rcv" + "  " + relative + " D    " + stringData[4].getBytes().length +" "+SN);
			data += stringData[4].getBytes().length;
			
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
				relative = System.currentTimeMillis() - start;
				log.println("snd" + "  " + relative + " A    " + 0 +" "+SN);
				
			//4-way connection teardown
				//FIN
			}else if (stringData[2].equals("true")){
				System.out.println("FIN received");
				recv++;
				relative = System.currentTimeMillis() - start;
				log.println("snd" + "  " + relative + " F    " + 0 +" "+SN);
				System.out.println("Connection teardown initiated...");
				
				//FINACK
				String[] FINACKS = heading(false, true, true, SN, senderACKno++);
				byte[] FINACK = StringAToByteA(FINACKS);
				DatagramPacket FINACKPacket = new DatagramPacket(FINACK, FINACK.length, clientHost, clientPort);
				receiver.send(FINACKPacket);
				System.out.println("FINACK sent");
				relative = System.currentTimeMillis() - start;
				log.println("snd" + "  " + relative + " FA   " + 0 +" "+SN);
				
				//ACK
				DatagramPacket ACKPacket = new DatagramPacket(new byte[1024], 1024);
				receiver.receive(ACKPacket);
				byte[] ACKB = ACKPacket.getData();
				String[] ACK = ByteAToStringA(ACKB);
				System.out.println(ACK[1]);
				if (ACK[1].equals("true")){
					System.out.println("ACK received");
					recv++;
					relative = System.currentTimeMillis() - start;
					log.println("snd" + "  " + relative + " A    " + 0 +" "+SN);
					receiver.close();
					//output string
					String trimmer = whole.trim();
					outputText(trimmer, outputFile);
					System.out.println("Socket closed");
					log.println("Total Data Received " + data + "\nData Segment Received " + recv);
					log.close();
					return;
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
