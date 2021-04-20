import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class peerThread extends Thread {
    private final Peer server;
    private RemotePeerInfo target;
    private Socket socket;
    private boolean initiator;
    private boolean isChoked;
    private  peerHandler pH;
    private byte[] myBitfield;
    private byte[] otherBitfield;
    Random random = new Random();

    private final DataOutputStream outputData;
    private final DataInputStream inputData;

    peerThread(Peer server, RemotePeerInfo target, Socket connectionSocket, boolean initiator, byte[] clientBitfield) throws IOException {
        this.target = target;
        this.server = server;
        this.socket = connectionSocket;
        this.initiator = initiator;
        this.myBitfield = clientBitfield;

        outputData = new DataOutputStream(socket.getOutputStream());
        inputData = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    @Override
    public void run() {
        try {
            // handshake message
            System.out.println("***beginning peer thread ****");

            int success = handshake();
            if (success < 0) {
                socket.close();
                return;
            }

            isChoked = true;
            while (true) {
                int length = inputData.available();
                byte[] message = new byte[length];
                inputData.readFully(message);
                decodeMessage(message);

                //READ INCOMING DATA, SEND CORRESPONDING MESSAGE
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
                System.out.println("Error! Cannot close socket");
            }
        }
    }

    private int handshake() throws IOException {

        if (initiator) {
            sendHandShake();
            int targetId = receiveHandshakeInitiator();

            if (targetId < 0) {
                socket.close();
                return -1;
            }
        } else {
            int targetId = receiveHandshake();
            if (targetId < 0) {
                socket.close();
                return -1;
            }
            //set the target
            target = Peer.getPeerInfoByID(server.peerInfoVector, targetId);
            sendHandShake();
        }

        return 0;
    }

    private void sendHandShake() {
        try {
            System.out.println(" **** ENTERING SEND HANDSHAKE ****");
            String messageOut = "P2PFILESHARINGPROJ" + "0000000000" + String.valueOf(server.peerID);
            System.out.println("MESSAGE: " + messageOut);
            outputData.write(messageOut.getBytes());
            outputData.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int receiveHandshake() throws IOException {
        System.out.println(" **** ENTERING RCV HANDSHAKE NON INITIATOR " + server.peerID + " ****");

        byte[] message = new byte[32];
        System.out.println("MESSAGE: " + new String(message));

        inputData.readFully(message);
        return verifyHandshake(message);

    }

    private int receiveHandshakeInitiator() throws IOException {
        byte[] message = new byte[32];
        inputData.readFully(message);
        String stringMessage = new String(message);
        System.out.println("TARGET: " + target.getPeerId() + " MESSAGE: " + stringMessage.substring(28, 32) + " END");

        int peerID = Integer.parseInt(stringMessage.substring(28, 32));
        if (verifyHandshake(message) == -1 || peerID != Integer.parseInt(target.getPeerId()))
            return -1;
        return peerID;
    }

    public int verifyHandshake(byte[] handshake) {
        String first18Bytes = Arrays.toString(handshake).substring(0, 18);
        if (!first18Bytes.equals("P2PFILESHARINGPROJ"))
            return -1;

        for (int i = 18; i < 28; i++) {
            if (handshake[i] != 0)
                return -1;
        }
        return Integer.parseInt(Arrays.toString(handshake).substring(28, 32));
    }

    public void decodeMessage(byte[] message) throws IOException {
        int length = Integer.parseInt(Arrays.toString(message).substring(0,4));
        int type = message[4];
        int byteIndex;

        switch(type) {
            case 0 : // choke - should be done
                isChoked = true;
                //logger.ChokedNeighbor(peerID1, peerID2);
                break;
            case 1 : // unchoke - should be done
                isChoked = false;
                request();
                //logger.UnchokedNeighbor(peerID1, peerID2);
                break;
            case 2 : // interested - should be done
                interested(Integer.parseInt(target.peerId));
                //logger.interestedMessage(peerID1, peerID2);
                break;
            case 3 : // not interested - should be done
                notInterested(Integer.parseInt(target.peerId));
                //logger.NotInterestedMessage(peerID1, peerID2);
                break;
            case 4 : // have - should be done
                int index = Integer.parseInt(Arrays.toString(message).substring(5,9));
                byteIndex = index / 8;
                otherBitfield[byteIndex] |= (int)Math.pow(2, (index % 8));
                verifyBitfield(Integer.parseInt(target.peerId), otherBitfield);
                if(!pH.interestedNeighbors.get(target.peerId)) {
                    String messageString = String.valueOf(1) + String.valueOf(2); // interested
                    byte[] newMessage = messageString.getBytes();
                    outputData.write(newMessage);
                    outputData.flush();
                }
                //logger.HaveMessage(peerID1, peerID2, index);
                break;
            case 5 : // bitfield - should be done
                otherBitfield = (Arrays.toString(message).substring(5,message.length)).getBytes();
                verifyBitfield(Integer.parseInt(target.peerId), otherBitfield);
                if(!pH.interestedNeighbors.get(target.peerId)) {
                    String messageString = String.valueOf(1) + String.valueOf(2); // interested
                    byte[] newMessage = messageString.getBytes();
                    outputData.write(newMessage);
                    outputData.flush();
                }
                else {
                    String messageString = String.valueOf(1) + String.valueOf(3); // not interested
                    byte[] newMessage = messageString.getBytes();
                    outputData.write(newMessage);
                    outputData.flush();
                }
                break;
            case 6 : // request - kinda of done
                int req_pieceIndex = Integer.parseInt(Arrays.toString(message).substring(5,9));
                byte[] piecePayload = getPiece(req_pieceIndex);
                // send piece
                String messageString = String.valueOf(1 + piecePayload.length) + String.valueOf(7); // not interested
                byte[] msgHeader = messageString.getBytes();
                byte[] newMessage = new byte[msgHeader.length + piecePayload.length];
                System.arraycopy(msgHeader, 0, newMessage, 0, msgHeader.length);
                System.arraycopy(piecePayload, 0, newMessage, msgHeader.length, piecePayload.length);
                outputData.write(newMessage);
                outputData.flush();
                break;
            case 7 : // piece - need to send HAVE to everyone
                int pieceIndex = Integer.parseInt(Arrays.toString(message).substring(5,9));
                byte[] pieceData = (Arrays.toString(message).substring(9,message.length)).getBytes();
                storePiece(pieceData, pieceIndex);

                byteIndex = pieceIndex / 8;
                otherBitfield[byteIndex] |= (int)Math.pow(2, (pieceIndex % 8));

                // send have to others

                if(!isChoked)
                    request();

                break;
            default :
                System.out.println("Invalid message type");
        }
    }

    void request() throws IOException {
        int nextPieceIndex = randRequestPiece(myBitfield, otherBitfield);
        String messageString;
        if(nextPieceIndex == -1)    // Interested msg
            messageString = String.valueOf(1) + String.valueOf(2);
        else    // Request msg
            messageString = String.valueOf(5) + String.valueOf(6) + String.valueOf(nextPieceIndex);
        byte[] message = messageString.getBytes();
        outputData.write(message);
        outputData.flush();
    }

    private void interested(int peerID) {
        pH.interestedNeighbors.put(peerID, true);
    }

    private void notInterested(int peerID) {
        pH.interestedNeighbors.put(peerID, false);
    }

    private void verifyBitfield(int peerID, byte[] otherBitfield) throws IOException {
        //if the serverBitfield has more set bits than the clientBitfield
        if((Integer.parseInt(myBitfield.toString()) | Integer.parseInt(otherBitfield.toString())) > Integer.parseInt(myBitfield.toString())){
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
            int fileSize = Integer.parseInt(Peer.getCommonData().elementAt(4).toString());
            int pieceSize = Integer.parseInt(Peer.getCommonData().elementAt(5).toString());
            FileInputStream fileStream = new FileInputStream(Peer.getCommonData().elementAt(3).toString());
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
            int pieceSize = Integer.parseInt(Peer.getCommonData().elementAt(5).toString());
            int pieceStart = pieceSize * index;
            RandomAccessFile fileStream = new RandomAccessFile(Peer.getCommonData().elementAt(3).toString(), "rw");
            fileStream.skipBytes(pieceStart);
            fileStream.write(piece);
            fileStream.close();
            //Update bitfield
        } catch(Exception e) {
            System.out.println(e);
        }
    }

//    public boolean isInteresting(byte[] bitfield, byte[] senderBitfield) {
//        byte[] unownedPieces = new byte[bitfield.length];
//        for(int i = 0; i < bitfield.length; i++) {
//            unownedPieces[i] = (byte) ((bitfield[i] ^ senderBitfield[i]) & senderBitfield[i]);
//            if(unownedPieces[i] != 0)
//                return true;
//        }
//        return false;
//    }

    public int randRequestPiece(byte[] bitfield, byte[] senderBitfield) {
        byte[] unownedPieces = new byte[bitfield.length];
        int zeroCount = 0;
        for(int i = 0; i < bitfield.length; i++) {
            unownedPieces[i] = (byte) ((bitfield[i] ^ senderBitfield[i]) & senderBitfield[i]);
            if(unownedPieces[i] == 0)
                zeroCount++;
        }

        if(zeroCount == unownedPieces.length)
            return -1;

        int byteIndex = random.nextInt(bitfield.length);
        while(unownedPieces[byteIndex] == 0)
            byteIndex = random.nextInt(bitfield.length);

        int bitIndex = random.nextInt(8);
        while(((int)unownedPieces[byteIndex] & (int)Math.pow(2, bitIndex)) == 0)
            bitIndex = random.nextInt(8);

        return byteIndex + bitIndex;
    }
}