import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class peerThread extends Thread {
    private final Peer server;
    private RemotePeerInfo client;
    private Socket connection;
    private boolean isServer;
    private boolean isChoked;
    private  peerHandler pH;
    private byte[] myBitfield;
    private byte[] otherBitfield;
    private log logger;
    Random random = new Random();
    private Vector<Peer> preferredNeighbors;
    private int pieceNum;

    private final DataOutputStream outputData;
    private final DataInputStream inputData;

    peerThread(Peer server, RemotePeerInfo client, Socket connectionSocket, boolean isServer, byte[] clientBitfield) throws IOException {
        this.client = client;
        this.server = server;
        this.connection = connectionSocket;
        this.isServer = isServer;
        this.myBitfield = clientBitfield;
        preferredNeighbors = new Vector<>(Integer.parseInt((Peer.getCommonData().elementAt(0)).toString()));
        pieceNum = 0;

        outputData = new DataOutputStream(connection.getOutputStream());
        inputData = new DataInputStream(new BufferedInputStream(connection.getInputStream()));
    }

    @Override
    public void run() {
        try {

            int connected = handshake();
            if (connected == 0) {
                connection.close();
                return;
            }

            logger.ConnectionReceived(server.peerID, Integer.parseInt(client.peerId));
            isChoked = true;

            pH.selectPreferredNeighbors(preferredNeighbors.size(), server.peerID);
            pH.optimisticallySelectNeighbor(server.peerID);

            while (true) {
                int length = inputData.available();
                byte[] message = new byte[length];
                inputData.readFully(message);
                decodeMessage(message);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int handshake() throws IOException {

        if (isServer) {
            sendHandShake();
            int targetId = receiveHandShakeServer();

            if (targetId == -1) {
                connection.close();
                return 0;
            }
        } else {
            int targetId = receiveHandShake();
            if (targetId == -1) {
                connection.close();
                return 0;
            }
            //set the target
            client = Peer.getPeerInfoByID(server.peerInfoVector, targetId);
            sendHandShake();
        }

        return 1;
    }

    private void sendHandShake() {
        try {
            String messageOut = "P2PFILESHARINGPROJ" + "0000000000" + server.peerID;
            outputData.write(messageOut.getBytes(), 0, 32);
            outputData.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int receiveHandShake() throws IOException {

        byte[] message = new byte[32];
        inputData.readFully(message);
        return verifyHandshake(message);

    }

    private int receiveHandShakeServer() throws IOException {
        try{
            byte[] message = new byte[32];
            inputData.readFully(message);
            String stringMessage = new String(message);
            int peerID = Integer.parseInt(stringMessage.substring(28, 32));
            if (verifyHandshake(message) == -1 || peerID != Integer.parseInt(client.getPeerId()))
                return -1;
            return peerID;
        }
        catch(EOFException e){
            System.err.println("EOF Exception!");
        }
        return -1;
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
                logger.ChokedNeighbor(server.peerID, Integer.parseInt(client.peerId));
                break;
            case 1 : // unchoke - should be done
                isChoked = false;
                request();
                logger.UnchokedNeighbor(server.peerID, Integer.parseInt(client.peerId));
                break;
            case 2 : // interested - should be done
                interested(Integer.parseInt(client.peerId));
                //logger.interestedMessage(peerID1, peerID2);
                break;
            case 3 : // not interested - should be done
                notInterested(Integer.parseInt(client.peerId));
                //logger.NotInterestedMessage(peerID1, peerID2);
                interested(Integer.parseInt(client.peerId));
                logger.InterestedMessage(server.peerID, Integer.parseInt(client.peerId));
                break;
            case 4 : // have - should be done
                int index = Integer.parseInt(Arrays.toString(message).substring(5,9));
                byteIndex = index / 8;
                otherBitfield[byteIndex] |= (int)Math.pow(2, (index % 8));
                verifyBitfield(Integer.parseInt(client.peerId), otherBitfield);
                if(!pH.interestedNeighbors.get(client.peerId)) {
                    String messageString = String.valueOf(1) + String.valueOf(2); // interested
                    byte[] newMessage = messageString.getBytes();
                    outputData.write(newMessage);
                    outputData.flush();
                }
                logger.HaveMessage(server.peerID, Integer.parseInt(client.peerId), index);
                break;
            case 5 : // bitfield - should be done
                otherBitfield = (Arrays.toString(message).substring(5,message.length)).getBytes();
                verifyBitfield(Integer.parseInt(client.peerId), otherBitfield);
                if(!pH.interestedNeighbors.get(client.peerId)) {
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
                setPiece(pieceData, pieceIndex);
                pieceNum++;
                logger.DownloadedPiece(server.peerID, Integer.parseInt(client.peerId), pieceIndex, pieceNum);

                byteIndex = pieceIndex / 8;
                myBitfield[byteIndex] |= (int)Math.pow(2, (pieceIndex % 8));
                checkDone();

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
        if(nextPieceIndex == -1)    // Not-interested msg
            messageString = String.valueOf(1) + String.valueOf(3);
        else    // Request msg
            messageString = String.valueOf(5) + String.valueOf(6) + String.valueOf(nextPieceIndex);
        byte[] message = messageString.getBytes();
        outputData.write(message);
        outputData.flush();
    }

    void checkDone() throws IOException {
        for(int i = 0; i < myBitfield.length; i++)
        {
            if((myBitfield[i] ^ 0) != 0)
                return;
        }
        server.setDone(String.valueOf(server.peerID));
        logger.CompletedDownload(server.peerID);
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
            fileStream.skipNBytes(pieceStart);
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

    public void setPiece(byte[] piece, int index) {
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