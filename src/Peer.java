
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Dictionary;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;
import java.io.*;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Random;
import java.util.Vector;

class Peer {
    private Client client;
    //private Server server;
    public int peerID;
    private log logger;
    private int clientBitfield;
    private Dictionary<Integer, Boolean> interestList;
    public Vector<RemotePeerInfo> peerInfoVector;
    Random random = new Random();

    private String pAddress;
    private int pPort;

    // vector to hold data from Common.cfg
    /*
        Index   Description
        0       NumberOfPreferredNeighbors
        1       UnchokingInterval
        2       OptimisticUnchokingInterval
        3       FileName
        4       FileSize
        5       PieceSize

     */
    private static Vector commonData;

    Peer(int peerID, Vector<RemotePeerInfo> peerInfoVector){
        this.pAddress = pAddress;
        this.pPort = pPort;
        this.peerID = peerID;
        this.peerInfoVector = peerInfoVector;

        client = new Client();
        //server = new Server(pAddress, pPort);
        logger = new log(peerID);
    }

    public void setCommonData() {
        String st;
        commonData = new Vector();

        try {
            BufferedReader in = new BufferedReader(new FileReader("project_config_file_small/Common.cfg"));
            while ((st = in.readLine()) != null) {

                String[] tokens = st.split("\\s+");

                commonData.add(tokens[1]);
            }

            in.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }

    }

    public static Vector getCommonData() {
        return commonData;
    }

    public void decodeMessage(byte[] message) {
        int length = Integer.parseInt(Arrays.toString(message).substring(0,4));
        int type = message[4];

        switch(type) {
            case 0 : // choke
                //choke(peerID);
                //logger.ChokedNeighbor(peerID1, peerID2);
                break;
            case 1 : // unchoke
                //unchoke(peerID);
                //logger.UnchokedNeighbor(peerID1, peerID2);
                break;
            case 2 : // interested
                interested(peerID);
                break;
            case 3 : // not interested
                notInterested(peerID);
                //logger.NotInterestedMessage(peerID1, peerID2);
                break;
            case 4 : // have
                int indexField = Integer.parseInt(Arrays.toString(message).substring(5,9));
                //have(peerID, have_pieceIndex);
                //logger.HaveMessage(peerID1, peerID2, index);
                break;
            case 5 : // bitfield
                int serverBitfield = Integer.parseInt(Arrays.toString(message).substring(5,message.length));
                verifyBitfield(peerID, serverBitfield);
                break;
            case 6 : // request
                int req_pieceIndex = Integer.parseInt(Arrays.toString(message).substring(5,9));
                //request(peerID, req_pieceIndex);
                break;
            case 7 : // piece
                int pieceIndex = Integer.parseInt(Arrays.toString(message).substring(5,9));
                int pieceData = Integer.parseInt(Arrays.toString(message).substring(9,message.length));
                //piece(peerID, pieceIndex, pieceData);
                break;
            default :
                System.out.println("Invalid message type");
        }
    }

    private void interested(int peerID) {
        interestList.put(peerID, true);
    }

    private void notInterested(int peerID) {
        interestList.put(peerID, false);
    }

    private void verifyBitfield(int peerID, int serverBitfield) {
        //if the serverBitfield has more set bits than the clientBitfield
        if((clientBitfield | serverBitfield) > clientBitfield){
            //send interested message
            interested(peerID);
        }
        else{
            //send uninterested message
            notInterested(peerID);
        }
    }
    public byte[] getPiece(int index) {
        try {
            int fileSize = Integer.parseInt(commonData.elementAt(4).toString());
            int pieceSize = Integer.parseInt(commonData.elementAt(5).toString());
            FileInputStream fileStream = new FileInputStream(commonData.elementAt(3).toString());
            long pieceStart = pieceSize * index;
            int length = pieceSize;
            // Check if we are at the end piece
            if(index == Math.ceil(fileSize/pieceSize))
                length = fileSize - (int)pieceStart;
            byte[] piece = new byte[length];
            fileStream.skip(pieceStart);
            long start = System.nanoTime();
            fileStream.read(piece);
            Client.setDownloadRate(System.nanoTime() - start);
            fileStream.close();
            return piece;
        } catch(Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public void storePiece(byte[] piece, int index) {
        try {
            int pieceSize = Integer.parseInt(commonData.elementAt(5).toString());
            int pieceStart = pieceSize * index;
            RandomAccessFile fileStream = new RandomAccessFile(commonData.elementAt(3).toString(), "rw");
            fileStream.skipBytes(pieceStart);
            fileStream.write(piece);
            fileStream.close();
            //Update bitfield
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    public boolean isInteresting(byte[] bitfield, byte[] senderBitfield) {
        byte[] unownedPieces = new byte[bitfield.length];
        for(int i = 0; i < bitfield.length; i++) {
            unownedPieces[i] = (byte) ((bitfield[i] ^ senderBitfield[i]) & senderBitfield[i]);
            if(unownedPieces[i] != 0)
                return true;
        }
        return false;
    }

    public int randRequestPiece(byte[] bitfield, byte[] senderBitfield) {
        byte[] unownedPieces = new byte[bitfield.length];
        for(int i = 0; i < bitfield.length; i++)
            unownedPieces[i] = (byte)((bitfield[i] ^ senderBitfield[i]) & senderBitfield[i]);

        int byteIndex = random.nextInt(bitfield.length);
        while(unownedPieces[byteIndex] == 0)
            byteIndex = random.nextInt(bitfield.length);

        int bitIndex = random.nextInt(8);
        while(((int)unownedPieces[byteIndex] & (int)Math.pow(2, bitIndex)) == 0)
            bitIndex = random.nextInt(8);

        return byteIndex + bitIndex;
    }

    public void start() throws IOException {
        System.out.println("BEGINNING PEER LOOP");
        for (int i = this.peerID; i >= 1001; i--) {
            if(this.peerID != i){
                System.out.println("PEER : " + this.peerID + " IN PEER LOOP ATTEMPTING CONNECTION WITH " + i);
                RemotePeerInfo targetPeer = getPeerInfoByID(peerInfoVector, i);

                Socket connectionSocket = connect(targetPeer);
                if (connectionSocket == null) continue;

                peerThread peerThread = new peerThread(this, targetPeer, connectionSocket, true);
                peerThread.start();
            }
        }
        //Thread to perform choking algorithm;
        //ChokeThread chokeThread = new ChokeThread();
        //chokeThread.start();

        //if (!downloadedFile)
        {
            //waitForBitField();
            //downloadedFile = true;
            //logger.fullyDownloaded();
        }

        //waitForNeighborBitField();
    }

    private Socket connect(RemotePeerInfo target)
    {
        try
        {
            // make connection to target
            Socket socket = new Socket(target.getPeerAddress(), Integer.parseInt(target.getPeerPort()));
            System.out.println("Connected to " + target.getPeerId() + " in port " + target.getPeerPort());

            return socket;
        }
        catch (ConnectException e)
        {
            System.err.println("Cannot connect! Connection exception.");
        }
        catch(UnknownHostException unknownHost)
        {
            System.err.println("Cannot connect! Host is unknown.");
        }
        catch(IOException ioException)
        {
            ioException.printStackTrace();
        }

        return null;
    }
    public static RemotePeerInfo getPeerInfoByID(Vector<RemotePeerInfo> peerVector, int peerId){
        RemotePeerInfo returnValue = null;
        for (RemotePeerInfo p: peerVector) {
            if (Integer.parseInt(p.getPeerId()) == peerId)
            {
                returnValue = p;
            }
        }
        return returnValue;
    }
}
