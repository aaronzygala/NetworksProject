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

    public void ChangedPreferredNeighbors(int peerID1, List<Peer> peers) {
        LocalTime time = LocalTime.now();
        try {
            output.write("[" + time.format(formatter) + "]: Peer " + peerID1 + " has the preferred neighbors ");
            for (int i = 0; i < peers.size() - 1; i++) {
                output.write(peers.get(i).peerID + ", ");
            }
            output.write(peers.get(peers.size() - 1).peerID + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void OptimisicUnchoke(int peerID1, int peerID2) {
        LocalTime time = LocalTime.now();
        try {
            output.write("[" + time.format(formatter) + "]: Peer " + peerID1 + " has the optimistically unchoked neighbor " + peerID2 + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void UnchokedNeighbor(int peerID1, int peerID2) {
        LocalTime time = LocalTime.now();
        try {
            output.write("[" + time.format(formatter) + "]: Peer " + peerID1 + " is unchoked by " + peerID2 + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void ChokedNeighbor(int peerID1, int peerID2) {
        LocalTime time = LocalTime.now();
        try {
            output.write("[" + time.format(formatter) + "]: Peer " + peerID1 + " is choked by " + peerID2 + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void HaveMessage(int peerID1, int peerID2, int index) {
        LocalTime time = LocalTime.now();
        try {
            output.write("[" + time.format(formatter) + "]: Peer " + peerID1 + " received the 'have' message from " + peerID2 + " for the piece " + index + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void InterestedMessage(int peerID1, int peerID2) {
        LocalTime time = LocalTime.now();
        try {
            output.write("[" + time.format(formatter) + "]: Peer " + peerID1 + " received the 'interested' message from " + peerID2 + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void NotInterestedMessage(int peerID1, int peerID2) {
        LocalTime time = LocalTime.now();
        try {
            output.write("[" + time.format(formatter) + "]: Peer " + peerID1 + " received the 'not interested' message from " + peerID2 + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void DownloadedPiece(int peerID1, int peerID2, int index, int num) {
        LocalTime time = LocalTime.now();
        try {
            output.write("[" + time.format(formatter) + "]: Peer " + peerID1 + " has downloaded the piece " + index + " from " + peerID2 + ". Now the number of pieces it has is " + num + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void CompletedDownload(int peerID1) {
        LocalTime time = LocalTime.now();
        try {
            output.write("[" + time.format(formatter) + "]: Peer " + peerID1 + " has downloaded the complete file.\n");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}