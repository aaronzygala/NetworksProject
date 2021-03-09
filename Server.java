import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.security.MessageDigest;
import java.util.*;

public class Server {

	private static final int sPort = 8000;   //The server will be listening on this port number

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running."); 
        ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 1;
        	try {
           		while(true) {
               		new Handler(listener.accept(),clientNum).start();
					System.out.println("Client "  + clientNum + " is connected!");
					clientNum++;
            	}
        	} finally {
            	listener.close();
        	} 
 
    	}

		/**
     	* A handler thread class.  Handlers are spawned from the listening
     	* loop and are responsible for dealing with a single client's requests.
     	*/
    	private static class Handler extends Thread {
			private String message;    //message received from the client
			private String MESSAGE;    //uppercase message send to the client
			private Socket connection;
			private ObjectInputStream in;	//stream read from the socket
			private ObjectOutputStream out;    //stream write to the socket
			private int no;		//The index number of the client

        	public Handler(Socket connection, int no) {
           		this.connection = connection;
	    		this.no = no;
        	}

        public void run() {
			try{
				//initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				try{
					while(true)
					{
						//receive the message sent from the client
						message = (String)in.readObject();
						//show the message to the user
						System.out.println("Receive message: " + message + " from client " + no);
						//Capitalize all letters in the message
						MESSAGE = message.toUpperCase();
						//send MESSAGE back to the client
						sendMessage(MESSAGE);
					}
				}
				catch(ClassNotFoundException classnot){
					System.err.println("Data received in unknown format");
				}
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Client " + no);
			}
			finally{
				//Close connections
				try{
					in.close();
					out.close();
					connection.close();
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client " + no);
				}
			}
		}

		//send a message to the output stream
		public void sendMessage(String msg)
		{
			try{
				out.writeObject(msg);
				out.flush();
				System.out.println("Send message: " + msg + " to Client " + no);
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

    }


	// ---------------------  PROJECT CODE  -----------------------------
	public int verifyHandshake(String handshake) {
		String first18Bytes = handshake.substring(0, 18);
		if(first18Bytes != "P2PFILESHARINGPROJ")
			return -1;
		
		for(int i = 18; i < 28; i++) {
			if((int)handshake.charAt(i) != 0)
				return -1;
		}

		int peerID = 0;
		for(int i = 31; i > 27; i--) {
			int temp = Character.getNumericValue(handshake.charAt(i));
			peerID += temp * Math.pow(10,(31-i));
		}

		return peerID;
	}

	public void decodeMessage(int message) {
		int length = message & 0xFFFFFFFF;
		int type = message & (0xFF << 4*8);

		switch(type) {
			case 0 : // choke
				//choke(peerID);
				break;
			case 1 : // unchoke
				//unchoke(peerID);
				break;
			case 2 : // interested
				//interested(peerID);
				break;
			case 3 : // not interested
				//not_interested(peerID);
				break;
			case 4 : // have
				int have_pieceIndex = message & (0xFFFFFFFF << 5*8);
				//have(peerID, have_pieceIndex);
				break;
			case 5 : // bitfield
				int payloadLength = length - 1; // 1 Byte for message type
				int bitfieldMask = 0;
				for(int i = 0; i < payloadLength; i++)
					bitfieldMask |= (0x1 << i);
				int bitfield = message & (bitfieldMask << 5*8);
				//bitfield(peerID, bitfield);
				break;
			case 6 : // request
				int req_pieceIndex = message & (0xFFFFFFFF << 5*8);;
				//request(peerID, req_pieceIndex);
				break;
			case 7 : // piece
				int pieceIndex = message & (0xFFFFFFFF << 5*8);;
				int pieceLength = length - 2; // 2 Bytes for message type and piece index
				int pieceMask = 0;
				for(int i = 0; i < pieceLength; i++)
					pieceMask |= (0x1 << i);
				int pieceData = message & (pieceMask << 6*8);
				//piece(peerID, pieceIndex, pieceData);
				break;
			default :
				System.out.println("Invalid message type");
		}
	}
}
