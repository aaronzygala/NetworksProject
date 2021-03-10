import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

class Peer {
    private Client client;
    private Server server;
    public int peerID;
    private log logger;
    private int clientBitfield;

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

    Peer(String pAddress, String pPort){
        client = new Client(pAddress, Integer.parseInt(pPort));
        server = new Server(pAddress, Integer.parseInt(pPort));
        logger = new log(peerID);
    }

    public static void getConfiguration(Vector commonData, String filepath) {
        String st;
        int i1;

        try {
            BufferedReader in = new BufferedReader(new FileReader(filepath));
            while ((st = in.readLine()) != null) {

                String[] tokens = st.split("\\s+");

                commonData.add(tokens[1]);
            }

            in.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }

    }

    // ---------------------  PROJECT CODE  -----------------------------
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
                //interested(peerID);
                break;
            case 3 : // not interested
                //notInterested(peerID);
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

    private void verifyBitfield(int peerID, int serverBitfield) {

        //if the serverBitfield has more set bits than the clientBitfield
        if((clientBitfield | serverBitfield) > clientBitfield){
            //send interested message
            //interested(peerID);
        }
        else{
            //send uninterested message
            //notInterested(peerID);
        }

    }

    public static void main(String[] args) {
        RemotePeerInfo peer = RemotePeerInfo(args[1],);

    }
}
