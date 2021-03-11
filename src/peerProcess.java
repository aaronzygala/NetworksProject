
import java.util.Dictionary;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

class Peer {
    private Client client;
    private Server server;
    public int peerID;
    private log logger;
    private int clientBitfield;
    private Dictionary<Integer, Boolean> interestList;

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
    private Vector commonData;


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


    public int verifyHandshake(String handshake) {
        String first18Bytes = handshake.substring(0, 18);
        if(!first18Bytes.equals("P2PFILESHARINGPROJ"))
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
                interested(peerID);
                break;
            case 3 : // not interested
                notInterested(peerID);
                break;
            case 4 : // have
                int have_pieceIndex = message & (0xFFFFFFFF << 5*8);
                //have(peerID, have_pieceIndex);
                break;
            case 5 : // bitfield
                int payloadLength = length - 1; // 1 Byte for message type
                int bitfieldMask = 0;
                for(int i = 0; i < payloadLength * 8; i++)
                    bitfieldMask |= (0x1 << i);
                int serverBitfield = message & (bitfieldMask << 5*8);
                verifyBitfield(peerID, serverBitfield);
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
