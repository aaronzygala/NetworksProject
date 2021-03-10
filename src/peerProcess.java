class Peer {
    private Client client;
    private Server server;
    private int peerID;
    private log logger;

    Peer(String pAddress, String pPort){
        client = new Client(pAddress, Integer.parseInt(pPort));
        server = new Server(pAddress, Integer.parseInt(pPort));
        logger = new log(peerID);
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
