package fr.ujm.ptorrent.client;

import fr.ujm.ptorrent.model.Book;
import fr.ujm.ptorrent.model.Library;
import fr.ujm.ptorrent.model.Player;
import fr.ujm.ptorrent.server.PlayerServer;
import fr.ujm.ptorrent.utils.JsonHelper;
import fr.ujm.ptorrent.utils.Librairifier;
import fr.ujm.ptorrent.utils.NetUtils;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

@Data
public class PlayerClient {
    private static PlayerClient playerClient;
    private Map<String, Socket> clientServerSockets = new HashMap<>();
    private Player player;

    private PlayerClient(Player player) {

        this.player = player;

        System.out.println("downloading ......");

        try {
            PlayerServer playerServer = new PlayerServer(player);
            playerServer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static PlayerClient getInstance(Player player) {
        if (playerClient == null)
            playerClient = new PlayerClient(player);
        return playerClient;
    }

    public boolean connectToHub(Library library) {

        try {
            System.out.println(library == null);
            if (library == null)
                return false;
            this.player.setLibrary(library);
            player.loadFilesFromHardrive();
            System.out.println("hub adress ip: " + library.getIp());
            player.setHubSocket(new Socket(library.getIp(), Integer.parseInt(library.getPort())));
            System.out.println("Connected to the hub");
            player.setPlayers(playerClient.getClientsListFromHub());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean connectToClient(Player otherPlayer) {
        try {
            System.out.println("conneting to player adress ip: " + otherPlayer.getIpAddress().getHostAddress());

            Socket socket = new Socket(otherPlayer.getIpAddress(), otherPlayer.getPort());
            clientServerSockets.put(otherPlayer.getId(), socket);
            System.out.println("Connected to the client");
            byte[] bitFieldMessage = NetUtils.readBytes(socket.getInputStream());
            System.out.println("received bitfield message" + Arrays.toString(bitFieldMessage));
            Player other = player.getPlayers().get(otherPlayer.getId());
            for (int i = 0; i < player.getLibrary().getHashes().size(); i++) {
                System.out.println("Bitfield: " + bitFieldMessage[i + 2]);
                if (bitFieldMessage[i + 2] == 1) {
                    other.getStuff().getBooks().put(player.getLibrary().getHashes().get(i), new Book(20000));
                }

            }

            return true;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }

    }


    // the player supposedly have loaded the library file and connected to hub
    public Map<String, Player> getClientsListFromHub() throws IOException {

        Socket hubSocket = player.getHubSocket();
        JsonHelper jsonHelper = new JsonHelper();
        String playerInfos = jsonHelper.jsonEncodePlayerTOHub(player) + "\r\n";
        System.out.println("sending player infos to the hub");
        System.out.println(playerInfos);
        hubSocket.getOutputStream().write(playerInfos.getBytes("utf-8"));
        System.out.println("still connected to hub? " + hubSocket.isConnected());
        System.out.println("ip: " + hubSocket.getInetAddress().getHostAddress());
        System.out.println("retrieving data from hub ...");
        InputStream inputStream = hubSocket.getInputStream();
        String jsonClientList = NetUtils.readLine(inputStream);
        out.println("player list in json : " + jsonClientList);
        return jsonHelper.decodeHubPlayerList(jsonClientList, player);

    }

    public void startDownload() {
        for (String key : this.player.getPlayers().keySet()) {
            System.out.println("key: " + key + " value: " + player.getPlayers().get(key));
            if (connectToClient(this.player.getPlayers().get(key))) {
                DownloadThread downloadThread = new DownloadThread(clientServerSockets, key, player);
                downloadThread.start();
            }

        }
    }
}

class DownloadThread extends Thread {
    private String key;
    private Map<String, Socket> clientServerSockets = new HashMap<>();
    private Player player;

    public DownloadThread(Map<String, Socket> clientServerSockets, String key, Player player) {
        this.clientServerSockets = clientServerSockets;
        this.key = key;
        this.player = player;
    }

    public void run() {
        while (true) {
            try {
                Socket socket = clientServerSockets.get(key);
                boolean haveAtLeastOnePiece = false;
                byte[] requestMessage = new byte[3];
                requestMessage[0] = (byte) (requestMessage.length - 1);
                requestMessage[1] = (byte) 1;
                System.out.println("player: " + key);
                System.out.println("numb of players: " + player.getPlayers().size());
                System.out.println("numb of other players boocks: " + player.getPlayers().get(key).getStuff().getBooks().size());
                System.out.println("numb of my books: " + player.getStuff().getBooks().size());
                for (int i = 0; i < player.getLibrary().getHashes().size(); i++) {
                    System.out.println("does the other player have the book? : " + player.getPlayers().get(key).getStuff().getBooks().containsKey(player.getLibrary().getHashes().get(i)));
                    System.out.println("do I have this book? " + player.getStuff().getBooks().containsKey(player.getLibrary().getHashes().get(i)));
                    if (player.getPlayers().get(key).getStuff().getBooks().containsKey(player.getLibrary().getHashes().get(i)) &&
                            !player.getStuff().getBooks().containsKey(player.getLibrary().getHashes().get(i))) {
                        requestMessage[2] = (byte) i;
                        player.getStuff().getBooks().put(player.getLibrary().getHashes().get(i), new Book(0));
                        haveAtLeastOnePiece = true;
                        break;

                    }

                }
                System.out.println("have a piece?: " + haveAtLeastOnePiece);
                if (haveAtLeastOnePiece) {
                    NetUtils.sendBytes(socket.getOutputStream(), requestMessage);
                    System.out.println("requestMessage: " + Arrays.toString(requestMessage));

                    byte[] pieceMessage = NetUtils.readBytes(socket.getInputStream());
                    System.out.println("pieceMessage ==: " + Arrays.toString(pieceMessage));
                    if (pieceMessage != null && pieceMessage[1] == 4) {
                        byte[] bookBytes = Arrays.copyOfRange(pieceMessage, 3, pieceMessage.length);
                        String sha1Book = Librairifier.bytesToSHA1(bookBytes);
                        System.out.println("book received converted to sha1: " + sha1Book);
                        System.out.println("verifying the bluff size:=" + player.getStuff().getBooks().get(player.getLibrary().getHashes().get(pieceMessage[2])).getSize());
                        if (player.getLibrary().getHashes().get(pieceMessage[2]).equals(sha1Book)) {
                            System.out.println("Sh1 verified");
                            Book book = new Book(bookBytes, bookBytes.length, pieceMessage[2]);
                            player.getStuff().getBooks().put(player.getLibrary().getHashes().get(pieceMessage[2]), book);
                            Librairifier librairifier = new Librairifier();
                            librairifier.saveBook(book, player.getLibrary().getFile());
                            byte[] haveMessage = new byte[3];
                            haveMessage[0] = (byte) (haveMessage.length - 1);
                            haveMessage[1] = (byte) 3;
                            haveMessage[2] = pieceMessage[2];
                            NetUtils.sendBytes(socket.getOutputStream(), haveMessage);
//                                            socket.getOutputStream().write(haveMessage);
                            System.out.println("requestMessage: " + Arrays.toString(haveMessage));
                        } else {
                            player.getStuff().getBooks().remove(player.getLibrary().getHashes().get(pieceMessage[2]));
                            System.out.println("********_______sh1 not valid_________*******");
                        }
                        System.out.println(" ======= Left Size is :  [" + player.getStuffleftSize() + "] ==========");
                        if (player.getStuffleftSize() <= 0) {
                            Librairifier librairifier = new Librairifier();
                            librairifier.booksToStuff(player.getStuff().getBooks(), player.getLibrary().getFile());
                        }
                    }
                } else {
                    System.out.println("This player doesnt have any piece to complete the puzzle! ");
                    break;
                }
//                            String input = NetUtils.readLine(socket.getInputStream());
//                        System.out.println(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}