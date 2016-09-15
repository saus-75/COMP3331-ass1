import java.io.*;
import java.net.*;

public class Receiver {
	public static void main(String[] args) throws Exception{
		if (args.length != 2){
			System.out.println("Required arguements: [Receiver_Port] [file.txt]\n");
			return;
		}
		int port = Integer.parseInt(args[0]);
		//String outputFile = args[1];
		
		DatagramSocket receiver = new DatagramSocket(port);
		receiver.setSoTimeout(500);
		while (receiver.isConnected()) {
			DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
			//receive
			receiver.receive(request);
			byte[] byteData = request.getData();
			String[] stringData = ByteAToStringA(byteData);
			int senderSeqNo = Integer.parseInt(stringData[3]);
			
			//reply
			String[] ACK = heading(false, true, 1, senderSeqNo+1);
			InetAddress clientHost = request.getAddress();
			int clientPort = request.getPort();
			byte[] replyBuf = StringAToByteA(ACK);
			DatagramPacket reply = new DatagramPacket(replyBuf, replyBuf.length, clientHost, clientPort);
			receiver.send(reply);
			
			System.out.println("Reply Sent!");
		}
		receiver.close();
	}
	
	public static String[] heading (boolean SYN, boolean ACK, int SeqNo, int AckNo){
		String[] head = new String[4];
		
		head[0] = Boolean.toString(SYN);
		head[1] = Boolean.toString(ACK);
		head[2] = Integer.toString(SeqNo);
		head[3] = Integer.toString(AckNo);
		
		return head;
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
