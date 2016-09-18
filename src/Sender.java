import java.io.*;
import java.net.*;

public class Sender {
	public static void main(String[] args){
		if (args.length != 8){
			System.out.println("Required arguements: [Receiver_Host_IP] [Receiver_Port] [file.txt] [MWS] [MMS] [Timeout] [PDrop] [seed]\n");
			return;
		}
		String host = args[0];
		InetAddress IP = InetAddress.getByName(host);
		int port = Integer.parseInt(args[1]);
		
		DatagramSocket senderSocket = new DatagramSocket();
	}
	
	public static String[] heading (boolean SYN, boolean ACK, int SeqNo, int AckNo){
		String[] head = new String[4];
		
		head[0] = Boolean.toString(SYN);
		head[1] = Boolean.toString(ACK);
		head[2] = Integer.toString(SeqNo);
		head[3] = Integer.toString(AckNo);
		
		return head;
	}
	
	// String cutter //
	
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
