
public class Packet {
	private int SeqNo;
	private int ACKSeqNo;
	private boolean ACK;
	private boolean SYN;
	//Need to sort out typing for message but this will do for now
	private String text;
	
	public Packet(int SeqNo, int ACKSeqNo, boolean ACK, boolean SYN, String text){
		this.SeqNo = SeqNo;
		this.ACKSeqNo = ACKSeqNo;
		this.ACK = ACK;
		this.SYN = SYN;
		this.text = text;
	}
	
	public int getSeqNo(){
		return SeqNo;
	}
	
	public int getACKSeqNo(){
		return ACKSeqNo;
	}
	
	public boolean getACK(){
		return ACK;
	}
	
	public boolean getSYN(){
		return SYN;
	}
	
	
}
