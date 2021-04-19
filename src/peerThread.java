import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

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
            int targetId = receiveHandshake();

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
            String messageOut = "P2PFILESHARINGPROJ" + "0000000000" + server.peerID;

            outputData.write(messageOut.getBytes());
            outputData.flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    private int receiveHandshake() throws IOException {
        byte[] message = new byte[32];
        inputData.readFully(message);
        return verifyHandshake(message);

    }
    public int verifyHandshake(byte[] handshake) {
        String first18Bytes = Arrays.toString(handshake).substring(0, 18);
        if(!first18Bytes.equals("P2PFILESHARINGPROJ"))
            return -1;

        for(int i = 18; i < 28; i++) {
            if(handshake[i] != 0)
                return -1;
        }
        return Integer.parseInt(Arrays.toString(handshake).substring(28,31));
    }
}
