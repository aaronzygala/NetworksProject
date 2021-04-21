/*
 *                     CEN5501C Project2
 * This is the program starting remote processes.
 * This program was only tested on CISE SunOS environment.
 * If you use another environment, for example, linux environment in CISE
 * or other environments not in CISE, it is not guaranteed to work properly.
 * It is your responsibility to adapt this program to your running environment.
 */

import com.jcraft.jsch.*;

import java.io.*;
import java.util.*;

/*
 * The StartRemotePeers class begins remote peer processes.
 * It reads configuration file PeerInfo.cfg and starts remote peer processes.
 * You must modify this program a little bit if your peer processes are written in C or C++.
 * Please look at the lines below the comment saying IMPORTANT.
 */
public class StartRemotePeers {

    public Vector<RemotePeerInfo> peerInfoVector;

    public void getConfiguration()
    {
        String st;
        int i1;
        peerInfoVector = new Vector<RemotePeerInfo>();
        System.out.println(System.getProperty("user.dir"));

        try {
            BufferedReader in = new BufferedReader(new FileReader("project_config_file_small\\PeerInfo.cfg"));
            while((st = in.readLine()) != null) {

                String[] tokens = st.split("\\s+");
                //System.out.println("tokens begin ----");
                //for (int x=0; x<tokens.length; x++) {
                //    System.out.println(tokens[x]);
                //}
                //System.out.println("tokens end ----");

                boolean hasFile;
                if(tokens[3] == "1")
                    hasFile = true;
                else
                    hasFile = false;
                peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2], hasFile));

            }

            in.close();
        }
        catch (Exception ex) {
            System.out.println(ex.toString());
        }

        System.out.println("Hello");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String ciseUser = "antoljak"; // change with your CISE username
        // TODO Auto-generated method stub
        try {
            StartRemotePeers myStart = new StartRemotePeers();
            myStart.getConfiguration();

            // get current path
            String path = System.getProperty("user.dir");

            // start clients at remote hosts
            for (RemotePeerInfo remotePeer : myStart.peerInfoVector) {
                try {
                    JSch jsch = new JSch();
                    /*
                     * Give the path to your private key. Make sure your public key
                     * is already within your remote CISE machine to ssh into it
                     * without a password. Or you can use the corressponding method
                     * of JSch which accepts a password.
                     */
                    jsch.addIdentity("C:\\Users\\lukaa\\.ssh\\id_rsa", "");
                    Session session = jsch.getSession(ciseUser, remotePeer.getPeerAddress(), 22);
                    Properties config = new Properties();
                    config.put("StrictHostKeyChecking", "no");
                    session.setConfig(config);

                    session.connect();

                    System.out.println("Session to peer# " + remotePeer.getPeerId() + " at " + remotePeer.getPeerAddress());

                    Channel channel = session.openChannel("exec");
                    //System.out.println("remotePeerID "+remotePeer.getPeerId());
                    ((ChannelExec) channel).setCommand("java peerProcess " + remotePeer.peerId);

                    channel.setInputStream(null);
                    ((ChannelExec) channel).setErrStream(System.err);

                    InputStream input = channel.getInputStream();
                    channel.connect();

                    //System.out.println("Channel Connected to peer# " + remotePeer.getPeerId() + " at "
                    //        + remotePeer.getPeerAddress());

                    (new Thread() {
                        @Override
                        public void run() {

                            InputStreamReader inputReader = new InputStreamReader(input);
                            BufferedReader bufferedReader = new BufferedReader(inputReader);
                            String line = null;

                            try {

                                while ((line = bufferedReader.readLine()) != null) {
                                    System.out.println(remotePeer.getPeerId() + ">:" + line);
                                }
                                bufferedReader.close();
                                inputReader.close();
                            } catch (Exception ex) {
                                System.out.println(remotePeer.getPeerId() + " Exception >:");
                                ex.printStackTrace();
                            }

                            channel.disconnect();
                            session.disconnect();
                        }
                    }).start();

                } catch (JSchException e) {
                    // TODO Auto-generated catch block
                    System.out.println(remotePeer.getPeerId() + " JSchException >:");
                    e.printStackTrace();
                } catch (IOException ex) {
                    System.out.println(remotePeer.getPeerId() + " Exception >:");
                    ex.printStackTrace();
                }
            }
            System.out.println("Starting all remote peers has done." );

        }
        catch (Exception ex) {
            System.out.println(ex);
        }
        System.out.println("olah");
    }
}