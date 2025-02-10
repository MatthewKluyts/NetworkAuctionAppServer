import java.util.*;
import java.util.concurrent.*;

public class Auction {
    private String itemName;
    public int highestBid = 0;
    public String highestBidder = "No bids yet";
    public boolean auctionEnded = false;
    private final Set<AuctionServer.ClientHandler> participants = ConcurrentHashMap.newKeySet();
    private final int auctionDuration;

    int remainingTime;



    public Auction(String itemName, int auctionDuration) {
        this.itemName = itemName;
        this.auctionDuration = auctionDuration;
        remainingTime = auctionDuration;
        startAuctionTimer();
    }

    public synchronized boolean placeBid(String bidder, int amount) {
        if (auctionEnded || amount <= highestBid) {
            return false;
        }
        highestBid = amount;
        highestBidder = bidder;
        remainingTime = Math.min(remainingTime + 10, auctionDuration + 60);
        broadcast("New highest bid for " + itemName + ": " + amount + " by " + bidder);
        broadcast("Time left for " + itemName + ": " + remainingTime + " seconds");
        return true;
    }

    public synchronized void addParticipant(AuctionServer.ClientHandler client) {
        if (participants.add(client)) { // `add` returns false if the client is already in the set
            client.sendMessage("Joined auction for: " + itemName);
            client.sendMessage("Current highest bid: " + highestBid);
        } else {
            client.sendMessage("Rejoined " + itemName + " auction.");
        }
    }

    private void startAuctionTimer() {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {

                while (remainingTime > 0 && !auctionEnded) {
                    if (remainingTime % 15 == 0 || remainingTime == auctionDuration || remainingTime == 0) {
                        broadcast("Time left for " + itemName + ": " + remainingTime + " seconds");
                    }
                    Thread.sleep(1000);
                    synchronized (Auction.class) {
                        remainingTime--;
                    }
                }
                endAuction();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private synchronized void endAuction() {
        auctionEnded = true;
        broadcast("Auction ended for " + itemName + ". Winner: " + highestBidder + " with bid: " + highestBid);
    }

    private void broadcast(String message) {
        for (AuctionServer.ClientHandler client : participants) {
            client.sendMessage(message);
        }
    }
}
