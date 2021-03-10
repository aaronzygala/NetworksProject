import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.security.MessageDigest;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class log {
    private FileWriter output;
    DateTimeFormatter formatter;

    log(int peerID) {
        try {
            output = new FileWriter("log_peer_" + peerID + ".log");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    }

    public void ConnectionMade(int peerID1, int peerID2) {
        LocalTime time = LocalTime.now();
        try {
            output.write("[" + time.format(formatter) + "]: Peer " + peerID1 + " makes a connection to Peer " + peerID2 + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void ConnectionReceived(int peerID1, int peerID2) {
        LocalTime time = LocalTime.now();
        try {
            output.write("[" + time.format(formatter) + "]: Peer " + peerID1 + " is connected from Peer " + peerID2 + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}