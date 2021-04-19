
import java.io.*;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Random;
import java.util.Vector;

class Peer {
    private Client client;
    private Server server;
    public int peerID;
    private log logger;
    private int clientBitfield;
    private Dictionary<Integer, Boolean> interestList;
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


    Peer(String pAddress, int pPort, int peerID){
        this.pAddress = pAddress;
        this.pPort = pPort;
        this.peerID = peerID;

        client = new Client();
        server = new Server(pAddress, pPort);
        logger = new log(peerID);
    }

    public void setCommonData() {
        String st;
        int i1;
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

    public static void getConfigurationPeerInfo(Vector peerInfoVector, String filepath) {
        String st;
        int i1;

        try {
            BufferedReader in = new BufferedReader(new FileReader(filepath));
            while ((st = in.readLine()) != null) {

                String[] tokens = st.split("\\s+");

                peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));
            }

            in.close();
        }
        catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    public int verifyHandshake(byte[] handshake) {
        String first18Bytes = Arrays.toString(handshake).substring(0, 18);
        if(!first18Bytes.equals("P2PFILESHARINGPROJ"))
            return -1;

        for(int i = 18; i < 28; i++) {
            if(handshake[i] != 0)
                return -1;
        }

        int peerID = Integer.parseInt(Arrays.toString(handshake).substring(28,31));
        return peerID;
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
            fileStream.read(piece);
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

    public static void main(String[] args) {

        // want to check if main is actually creating a process
        System.out.print("Hello I am being called within peerProcess with ID: ");
        Peer peer;
        if(args.length != 0) {
            System.out.println(Integer.parseInt(args[1]));

            // initializing peer with unique port but all on localhost so we can test it!
            //                        Address      port                       id
            peer = new Peer("localhost", Integer.parseInt(args[1]), Integer.parseInt(args[0]));
        }
        // if we don't pass data
        else {
            System.out.println(1002);

            // initializing peer with unique port but all on localhost so we can test it!
            //                         Address      port                id
            peer = new Peer("localhost", 8001, 1002);
        }

        Vector<RemotePeerInfo> peerInfoVector = new Vector<RemotePeerInfo>();
        String peerInfo = "project_config_file_small/PeerInfo.cfg";
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        try {
            // initialize common vector to initialize data from Common.cfg
            peer.setCommonData();

            // initialize peerInfoVector to hold data about each peer
            getConfigurationPeerInfo(peerInfoVector, peerInfo);

            // start server side so peer can listen to new requests
            // this start method is apart of the Thread class. it calls the run()
            // function which overrides the run() function originally belonging to Thread
            // this spawns a thread which just runs run(), thread closes automatically when run()
            // finishes
            //peer.server.start();

            // by project specification establish connections to each peer
           // for(int port = 8001; port <= 8009; port++) {

             //   peer.client.run("localhost", port);
           // }


        }
        catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("end of peerProcess");

    }
}
