import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class AuctionServer {
    private static final int PORT = 12345;
    private static AuctionManager auctionManager = new AuctionManager();

    public static void main(String[] args) {
        System.out.println("Auction server started...");
        auctionManager.createAuction("Laptop", 10000);
        auctionManager.createAuction("Phone", 10000);
        auctionManager.createAuction("Tablet", 10000);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                handler.start(); // Start the thread for this client
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        private Auction currentAuction;

        private ClientHandler clientHandlerCurrent;


        private BlockingQueue<String> outgoingMessages;

        ReadThread readThread;
        WriteThread writeThread;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.outgoingMessages = new LinkedBlockingQueue<>();
            this.clientHandlerCurrent = this;
        }

        private void auctionEnded(){
            sendMessage("Auction has ended. No more bids can be placed.");
        }

        private void joinAuctionMessage(){
            sendMessage("Join an auction first.");
        }

        private void InvalidBid(){

            sendMessage("Invalid bid. Current highest bid: " + currentAuction.highestBid);

        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                readThread = new ReadThread(clientHandlerCurrent);
                writeThread = new WriteThread();

                readThread.start();
                writeThread.start();


            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }

        private class ReadThread extends Thread {

            private ClientHandler currentClient;

            public ReadThread(ClientHandler clientHandler)
            {
                this.currentClient = clientHandler;
            }

            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        // Blocking operation to read messages from the client
                        String message = in.readLine();

                         if (message.startsWith("Bid placed: ")) {

                            if (currentAuction != null) {
                                try {

                                    if (currentAuction.auctionEnded) {
                                        auctionEnded();

                                    }
                                    else
                                    {
                                        int bidAmount = Integer.parseInt(message.substring(12).trim());
                                        if (!currentAuction.placeBid(clientName, bidAmount)) {
                                            InvalidBid();
                                        }
                                    }

                                } catch (NumberFormatException e) {

                                }
                            } else {
                                joinAuctionMessage();
                            }
                        } else if (message.startsWith("JOIN ")){
                            String itemName = message.substring(5).trim();
                            Auction auction = auctionManager.getAuction(itemName);
                            if (auction != null) {
                                currentAuction = auction;
                                auction.addParticipant(currentClient);
                            } else {
                                out.println("Invalid auction: " + itemName);
                            }
                        }
                        else
                        {
                            clientName = message;
                            System.out.println(clientName + " connected successfully.");
                            //welcomeClient();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    cleanup();
                }
            }
        }

        private class WriteThread extends Thread {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        // Blocking operation to dequeue messages and send
                        String message = outgoingMessages.take();
                        out.println(message);


                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    cleanup();
                }
            }
        }



        private void cleanup() {
            try {
                if (socket != null) socket.close();
                if (readThread != null) readThread.interrupt();
                if (writeThread != null) writeThread.interrupt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            outgoingMessages.offer(message);
        }
    }
}
