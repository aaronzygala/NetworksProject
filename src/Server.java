import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.security.MessageDigest;
import java.util.*;


// a bit tricky what was done here
// server gets spawned as a thread from peerProcess
// server, now a thread runs run()
// then inside server.run() it spawns more threads
// a thread as a listener, and a new thread with each incoming connection

public class Server extends Thread {

    private int pPort;   //The server will be listening on this port number
    private String pAddress;
    private ServerSocket listener;

    Server(String pAddress, int pPort) {

        this.pAddress = pAddress;
        this.pPort = pPort;
    }

    public void run() {

        try{
            System.out.println("The server is running.");
            // create a serversocket object
            listener = new ServerSocket();
            // bind the corresponding port and host (IP)
            listener.bind(new InetSocketAddress(pAddress, pPort));
            int clientNum = 1;

            while(true) {
                new Handler(listener.accept(),clientNum).start();
                System.out.println("Client "  + clientNum + " is connected!");
                clientNum++;
            }


        } catch(Exception e) {
            System.out.println(e);

        } finally {
            try { listener.close(); }
            catch (IOException e) { e.printStackTrace(); }
        }

    }


    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    private static class Handler extends Thread {
        private String message;    //message received from the client
        private String MESSAGE;    //uppercase message send to the client
        private Socket connection;
        private ObjectInputStream in;	//stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
        private int no;		//The index number of the client

        public Handler(Socket connection, int no) {
            this.connection = connection;
            this.no = no;
        }

        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());
                try{
                    while(true)
                    {
                        //receive the message sent from the client
                        message = (String)in.readObject();
                        //show the message to the user
                        System.out.println("Receive message: " + message + " from client " + no);
                        //Capitalize all letters in the message
                        MESSAGE = message.toUpperCase();
                        //send MESSAGE back to the client
                        sendMessage(MESSAGE);
                    }
                }
                catch(ClassNotFoundException classnot){
                    System.err.println("Data received in unknown format");
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + no);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }

        //send a message to the output stream
        public void sendMessage(String msg)
        {
            try{
                out.writeObject(msg);
                out.flush();
                System.out.println("Send message: " + msg + " to Client " + no);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }

    }
}
