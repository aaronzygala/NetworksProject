
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.*;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

class Peer {
    private Client client;
    //private Server server;
    public int peerID;
    private log logger;
    private byte[] myBitfield;
    private HashMap<String, Boolean> peerMap;
    public Vector<RemotePeerInfo> peerInfoVector;
    private ServerSocket serverListener;
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
        this.peerID = peerID;
        this.peerInfoVector = peerInfoVector;

        client = new Client();
        //server = new Server(pAddress, pPort);
        logger = new log(peerID);

        int count = 0;
        for (RemotePeerInfo p : peerInfoVector) {
            count++;
        }

        peerMap = new HashMap<>(count);
        for (RemotePeerInfo p : peerInfoVector) {
            peerMap.put(p.peerId, p.hasFile);
        }
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

    public void start() throws IOException, InterruptedException {
        RemotePeerInfo info = getPeerInfoByID(this.peerInfoVector, this.peerID);
        int port = Integer.parseInt(info.getPeerPort());
        Peer p = this;
        serverListener = new ServerSocket(port);
        (new Thread() {
            public void run() {
                try {
                    System.out.println("Waiting for clients at " + port);

                    while (true)
                    {
                        Socket connectionSocket = serverListener.accept();
                        System.out.println("new connection");

                        peerThread pThread = new peerThread(p, null, connectionSocket, false, p.myBitfield);
                        new Thread(pThread).start();
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }).start();

        for (int i = this.peerID; i >= 1001; i--) {
            if(this.peerID == 1001){
                Thread.sleep(2000);
            }
            if(this.peerID != i){
                System.out.println("PEER : " + this.peerID + " IN PEER LOOP ATTEMPTING CONNECTION WITH " + i);
                RemotePeerInfo targetPeer = getPeerInfoByID(peerInfoVector, i);

                Socket connectionSocket = connect(targetPeer);
                if (connectionSocket == null) continue;

                peerThread peerThread = new peerThread(this, targetPeer, connectionSocket, true, myBitfield);
                peerThread.start();
            }
        }

        while(true) {
            boolean allDone = false;
            for(boolean hasFile : peerMap.values()) {
                if(!hasFile) {
                    allDone = false;
                    break;
                }
                allDone = true;
            }
            if(allDone)
                break;
        }

        serverListener.close();
    }

    private Socket connect(RemotePeerInfo target)
    {
        try {
            Socket socket = new Socket(target.getPeerAddress(), Integer.parseInt(target.getPeerPort()));
            System.out.println("Connected to " + target.getPeerId() + " in port " + target.getPeerPort());
            return socket;
        }
        catch (ConnectException e) {
            System.out.println("Cannot connect! Connection exception.");
        }
        catch(UnknownHostException unknownHost) {
            System.out.println("Cannot connect! Host is unknown.");
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static RemotePeerInfo getPeerInfoByID(Vector<RemotePeerInfo> peerVector, int peerId){
        RemotePeerInfo returnValue = null;
        for (RemotePeerInfo p: peerVector) {
            if (Integer.parseInt(p.getPeerId()) == peerId) {
                returnValue = p;
            }
        }
        return returnValue;
    }

    public void setDone(String peerID) {
        peerMap.put(peerID, true);
    }
}
