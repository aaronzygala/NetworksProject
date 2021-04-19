import java.util.*;

public class peerHandler {
    private Map<Integer, Client> peerMap;
    private List<Client> preferredNeighbors;
    private List<Client> interestedNeighbors;
    private Client optimisticallyUnchokedNeighbor;
    boolean hasFile;
    Random random = new Random();

    public synchronized void selectPreferredNeighbors(int NumberOfPreferredNeighbors) throws InterruptedException {
        long timer = Integer.parseInt(Peer.getCommonData().elementAt(1).toString()) * 1000;

        new Thread() {
            public void run() {
                try {
                    preferredNeighbors.clear();

                    for(int i = 0; i < NumberOfPreferredNeighbors; i++) {
                        
                        if(!hasFile) {
                            Client fastestDLRate = peerMap.get(0);

                            for(Map.Entry<Integer, Client> peer : peerMap.entrySet()) {
                                if(peer.getValue().downloadRate == fastestDLRate.downloadRate
                                && !preferredNeighbors.contains(peer.getValue())
                                && interestedNeighbors.contains(peer.getValue())) {
                                    if(random.nextInt(2) == 0) // 0 or 1
                                        fastestDLRate = peer.getValue();
                                }
                                else if(peer.getValue().downloadRate > fastestDLRate.downloadRate
                                && !preferredNeighbors.contains(peer.getValue())
                                && interestedNeighbors.contains(peer.getValue()))
                                    fastestDLRate = peer.getValue();
                            }
                            preferredNeighbors.add(fastestDLRate);
                        }
                        else {  // If peer has file, choose randomly
                            int index = random.nextInt(peerMap.size());
                            while(preferredNeighbors.contains(peerMap.get(index)) || !interestedNeighbors.contains(peerMap.get(index)))
                                index = random.nextInt(peerMap.size());
                            preferredNeighbors.add(peerMap.get(index));
                        }
                    }

                    for(Map.Entry<Integer, Client> peer : peerMap.entrySet()) {
                        if(preferredNeighbors.contains(peer)) // && peer.choked == true
                            continue; // unchoke(peer)
                        else if(!preferredNeighbors.contains(peer)) // && peer.choked == false
                            continue; // choke(peer)
                    }
                } catch(Exception e) {
                    System.out.println(e);
                }
            }
        };
        Thread.sleep(timer);
    }

    public synchronized void optimisticallySelectNeighbor() throws InterruptedException {
        long timer = Integer.parseInt(Peer.getCommonData().elementAt(2).toString()) * 1000;

        new Thread() {
            public void run() {
                try {
                    int index = random.nextInt(peerMap.size());
                    // while(peerMap.get(index).choked == false)
                        index = random.nextInt(peerMap.size());
                    // peerMap.get(index).unchoke();
                    optimisticallyUnchokedNeighbor = peerMap.get(index);  
                } catch(Exception e) {
                    System.out.println(e);
                }
            }
        };
        Thread.sleep(timer);
    }
}
