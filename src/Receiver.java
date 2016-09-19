import java.io.*;
import java.net.*;

public class Receiver {
	private static final int SEQ_NUM = 31;
	public static void main(String[] args) throws Exception{
		if (args.length != 2){
			System.out.println("Required arguements: [Receiver_Port] [file.txt]\n");
			return;
		}
		int port = Integer.parseInt(args[0]);
		//String outputFile = args[1];
		
		DatagramSocket receiver = new DatagramSocket(port);
		
		String whole = "";
		
		while (true) {
			DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
			//receive
			receiver.receive(request);
			byte[] byteData = request.getData();
			String[] stringData = ByteAToStringA(byteData);
			
			int senderSeqNo = Integer.parseInt(stringData[3]);
			
			//String concat
			String back = stringData[5];
			whole += back;
			
			//reply
			String[] ACK = heading(false, true, false, SEQ_NUM, senderSeqNo+1);
			InetAddress clientHost = request.getAddress();
			int clientPort = request.getPort();
			byte[] replyBuf = StringAToByteA(ACK);
			DatagramPacket reply = new DatagramPacket(replyBuf, replyBuf.length, clientHost, clientPort);
			receiver.send(reply);
			
			System.out.println("Reply Sent!");
			if (stringData[2].equals(true)){
				receiver.close();
				break;
			}
		}
		
		try{
			String trimmer = whole.trim();
			outputText(trimmer);
		} catch (FileNotFoundException e){
			System.err.println("text empty.");
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
	public static void outputText(String text) throws FileNotFoundException{
		PrintWriter out = new PrintWriter(text);
		out.print(text);
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
