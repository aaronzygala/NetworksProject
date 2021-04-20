import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

public class peerProcess {

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
    public static void main(String[] args) {

        Peer peer;
        int peerId = 1002;
        //int portNumber = 8001;
        if(args.length != 0) {
            peerId = Integer.parseInt(args[0]);
            //portNumber = Integer.parseInt(args[1]);
        }
        else {
            System.out.println("No command line arguments.");
        }

        Vector<RemotePeerInfo> peerInfoVector = new Vector<RemotePeerInfo>();
        String peerInfo = "project_config_file_small/PeerInfo.cfg";

        try {
            // initialize peerInfoVector to hold data about each peer
            getConfigurationPeerInfo(peerInfoVector, peerInfo);

            peer = new Peer(peerId, peerInfoVector);

            // initialize common vector to initialize data from Common.cfg
            peer.setCommonData();

            peer.start();
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
