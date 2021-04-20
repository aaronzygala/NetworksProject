import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class peerThread extends Thread{
    private final Peer server;
    private RemotePeerInfo target;
    private Socket socket;
    private boolean initiator;

    private final DataOutputStream outputData;
    private final DataInputStream inputData;

    peerThread(Peer server, RemotePeerInfo target, Socket connectionSocket, boolean initiator) throws IOException {
        this.target = target;
        this.server = server;
        this.socket = connectionSocket;
        this.initiator = initiator;

        outputData = new DataOutputStream(socket.getOutputStream());
        inputData = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    @Override
    public void run() {
        try
        {
            // handshake message
            System.out.println("***beginning peer thread ****");

            int success = handshake();
            if (success < 0)
            {
                socket.close();
                return;
            }

            while (true)
            {
                //READ INCOMING DATA, SEND CORRESPONDING MESSAGE
            }

        } catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                socket.close();
            } catch (IOException e1)
            {
                e1.printStackTrace();
                System.out.println("Error! Cannot close socket");
            }
        }
    }

    private int handshake() throws IOException {

        if (initiator)
        {
            sendHandShake();
            int targetId = receiveHandshakeInitiator();

            if (targetId < 0)
            {
                socket.close();
                return -1;
            }
        }
        else
        {
            int targetId = receiveHandshake();
            if (targetId < 0)
            {
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
        try
        {
            System.out.println(" **** ENTERING SEND HANDSHAKE ****");
            String messageOut = "P2PFILESHARINGPROJ" + "0000000000" + server.peerID;
            System.out.println("MESSAGE: " + messageOut);
            outputData.write(messageOut.getBytes());
            outputData.flush();
        }
        catch (IOException e)
        {
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
        System.out.println("TARGET: " + target.getPeerId() + " MESSAGE: " + stringMessage.substring(28,32) + " END");

        int peerID = Integer.parseInt(stringMessage.substring(28,32));
        if(verifyHandshake(message) == -1 || peerID != Integer.parseInt(target.getPeerId()))
            return -1;
        return peerID;
    }
    public int verifyHandshake(byte[] handshake) {
        String first18Bytes = Arrays.toString(handshake).substring(0, 18);
        if(!first18Bytes.equals("P2PFILESHARINGPROJ"))
            return -1;

        for(int i = 18; i < 28; i++) {
            if(handshake[i] != 0)
                return -1;
        }
        return Integer.parseInt(Arrays.toString(handshake).substring(28,32));
    }
}
