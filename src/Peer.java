
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
import java.util.concurrent.TimeUnit;

class Peer {
    private Client client;
    //private Server server;
    public int peerID;
    private log logger;
    private byte[] clientBitfield;
    private Dictionary<Integer, Boolean> interestList;
    public Vector<RemotePeerInfo> peerInfoVector;

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

    public void start() throws IOException, InterruptedException {
        System.out.println("BEGINNING PEER LOOP");
        for (int i = this.peerID; i >= 1001; i--) {
            if(this.peerID != i){
                System.out.println("PEER : " + this.peerID + " IN PEER LOOP ATTEMPTING CONNECTION WITH " + i);
                RemotePeerInfo targetPeer = getPeerInfoByID(peerInfoVector, i);

                Socket connectionSocket = connect(targetPeer);
                if (connectionSocket == null) continue;

                peerThread peerThread = new peerThread(this, targetPeer, connectionSocket, true, clientBitfield);
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
