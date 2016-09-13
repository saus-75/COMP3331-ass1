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
		while (true) {
			DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
			//receive
			receiver.receive(request);
			
			//reply
			Packet ack = new Packet(1, 2, true, false, null);
			InetAddress clientHost = request.getAddress();
			int clientPort = request.getPort();
			byte[] replyBuf = null;
			DatagramPacket reply = new DatagramPacket(replyBuf, replyBuf.length, clientHost, clientPort);
			receiver.send(reply);
			
			System.out.println("Reply Sent!");
		}
	}
		
}
