import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Client {
    private Socket requestSocket;           //socket connect to the server
    private ObjectOutputStream out;         //stream write to the socket
    private ObjectInputStream in;          //stream read from the socket
    private String message;                //message send to the server
    private String MESSAGE;                //capitalized message read from the server
    long downloadRate;

    // corresponding peer address and port
    private String pAddress;
    private int pPort;


    Client(String pAddress, int pPort) {
        this.pAddress = pAddress;
        this.pPort = pPort;
    }

    // function to get message to do work in Peer object
    String getMESSAGE(){ return MESSAGE; }

    // function to run program by making socket and doing requests
    void run()
    {
        try{
            //create a socket to connect to the server
            requestSocket = new Socket(pAddress , pPort);
            System.out.println("Connected to localhost in port " + pPort);
            //initialize inputStream and outputStream
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());

            //get Input from standard input
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while(true)
            {
                System.out.print("Hello, please input a sentence: ");
                //read a sentence from the standard input
                message = bufferedReader.readLine();
                //Send the sentence to the server
                sendMessage(message);
                //Receive the upperCase sentence from the server
                MESSAGE = (String)in.readObject();
                //show the message to the user
                System.out.println("Receive message: " + MESSAGE);
            }
        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        catch ( ClassNotFoundException e ) {
            System.err.println("Class not found");
        }
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        finally{
            //Close connections
            try{
                in.close();
                out.close();
                requestSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }
    //send a message to the output stream
    void sendMessage(String msg)
    {
        try{
            //stream write the message
            out.writeObject(msg);
            out.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
    //main method
    /*
    public static void main(String args[])
    {
        Client client = new Client();
        client.run();
    }

    */
}
