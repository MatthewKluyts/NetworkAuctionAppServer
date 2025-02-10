import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private Map<String, Auction> auctions = new ConcurrentHashMap<>();

    public synchronized Auction createAuction(String itemName, int duration) {
        Auction auction = new Auction(itemName, duration);
        auctions.put(itemName, auction);
        return auction;
    }

    public Auction getAuction(String itemName) {
        return auctions.get(itemName);
    }

}

