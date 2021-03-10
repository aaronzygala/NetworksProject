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

    public static void main(String[] args) {
        Vector<RemotePeerInfo> peerInfoVector = new Vector<RemotePeerInfo>();
        Vector<Vector<String>> commonVector = new Vector<>();

        String peerInfo = "project_config_file_small/PeerInfo.cfg";

        try {
            getConfiguration(peerInfoVector, peerInfo);

            for (int i = 0; i < peerInfoVector.size(); i++)
                Runtime.getRuntime().exec("java peerProcess " + peerInfoVector.elementAt(i).peerId);
        } catch (Exception e){
            System.out.println(e);
        }

        System.out.println("hello!");
    }

}
