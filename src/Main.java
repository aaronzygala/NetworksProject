import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

public class Main {


    public static void getConfiguration(Vector peerInfoVector, String filepath) {
        String st;
        int i1;

        try {
            BufferedReader in = new BufferedReader(new FileReader(filepath));
            while ((st = in.readLine()) != null) {

                String[] tokens = st.split("\\s+");

                peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));
            }

            in.close();
        } catch (Exception ex) { System.out.println(ex.toString()); }
    }

    // function to make testing setup. ports 8001 to 8009 inclusive
    public static void getConfigurationTest(Vector peerInfoVector, String filepath) {
        String st;
        int i1, i = 1;

        try {
            BufferedReader in = new BufferedReader(new FileReader(filepath));
            while ((st = in.readLine()) != null) {

                String[] tokens = st.split("\\s+");

                peerInfoVector.addElement(new RemotePeerInfo(tokens[0], "localhost", Integer.toString(8000 + i++)));
            }

            in.close();
        } catch (Exception ex) { System.out.println(ex.toString()); }
    }

    public static void main(String[] args) {
        Vector<RemotePeerInfo> peerInfoVector = new Vector<RemotePeerInfo>();

        String peerInfo = "project_config_file_small/PeerInfo.cfg";

        try {
            getConfigurationTest(peerInfoVector, peerInfo);

            // uncomment to know what current directory you are in
            //System.out.println("Working Directory = " + System.getProperty("user.dir"));

            // run command to run process. create all 9 processes so they can communicate with each other
            for (int i = 0; i < peerInfoVector.size(); i++) {

                // specify the classpath to Peer.class (the executable) and then reference the executable Peer
                // runs a terminal command: java -classpath out/production/NetworksProject Peer <peer id> <peer port>
                Runtime.getRuntime().exec("java -classpath out/production/NetworksProject Peer  "
                        + peerInfoVector.elementAt(i).peerId + " " + peerInfoVector.elementAt(i).peerPort);

            }

        } catch (Exception e){
            System.out.println(e);
        }

        System.out.println("hello!");
    }

}
