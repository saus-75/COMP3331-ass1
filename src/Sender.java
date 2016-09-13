import java.io.*;
import java.net.*;

public class Sender {
	public static void main(String[] args) throws Exception{
		if (args.length != 8){
			System.out.println("Required arguements: [Receiver_Host_IP] [Receiver_Port] [file.txt] [MWS] [MMS] [Timeout] [PDrop] [seed]\n");
			return;
		}
		
		String host = args[0];
		InetAddress ServerIPAddress = InetAddress.getByName(host);
		int port = Integer.parseInt(args[1]);
		
		DatagramSocket sender = new DatagramSocket();
		
		textSplitter();
		
		while ()
	}
}
