package fr.ujm.ptorrent.server;

import fr.ujm.ptorrent.client.PlayerClient;
import fr.ujm.ptorrent.model.Book;
import fr.ujm.ptorrent.model.Player;
import fr.ujm.ptorrent.utils.NetUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.out;

public class PlayerServer extends Thread {

    private final Logger LOGGER = Logger.getLogger(PlayerServer.class.getName());
    private ServerSocket serverSocket;
    private Socket socket;
    private Player player;

    public PlayerServer(Player player) throws IOException {

        serverSocket = new ServerSocket(player.getPort(), -1, player.getIpAddress());
        this.player = player;

    }

    public void run() {

        LOGGER.log(Level.INFO, "Starting the player server ....");
        System.out.println(serverSocket.getInetAddress().getHostAddress());
        // process clients one by one (not in parallel)
        while (true) {
            try {
                socket = serverSocket.accept();
                PlayerClient playerClient = PlayerClient.getInstance(player);
                if ("-1".equals(player.getHashByIp(socket.getRemoteSocketAddress().toString().substring(1).split(":")[0])))
                    player.setPlayers(playerClient.getClientsListFromHub());
                out.println("ACCEPTING CONNECTIONS FROM :" + socket.getRemoteSocketAddress());
                byte[] bitFieldMessage = new byte[player.getLibrary().getHashes().size() + 2];
                bitFieldMessage[0] = (byte) (bitFieldMessage.length - 1);
                bitFieldMessage[1] = (byte) 2;
                System.out.println("Books in possession: " + player.getStuff().getBooks().size());
                for (int i = 0; i < player.getLibrary().getHashes().size(); i++) {
                    if (player.getStuff().getBooks().containsKey(player.getLibrary().getHashes().get(i)))
                        bitFieldMessage[i + 2] = (byte) 1;
                    else
                        bitFieldMessage[i + 2] = (byte) 0;

                }
                System.out.println("sending the bitfield message: " + Arrays.toString(bitFieldMessage) + "\r\n");
//                DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
//
//                dOut.writeInt(message.length); // write length of the message
//                dOut.write(message);
                NetUtils.sendBytes(socket.getOutputStream(), bitFieldMessage);
//                socket.getOutputStream().write(bitFieldMessage);

                new Thread() {
                    public void run() {
                        try {
                            while (true) {

//                                stuff.getBooks().size();
//                                System.out.println("sending the bitfield message:");
//                                socket.getOutputStream().write("this is what I have bb \r\n".getBytes("utf-8"));
                                synchronized (this) {
                                    byte[] request = NetUtils.readBytes(socket.getInputStream());
                                    System.out.println("request: " + Arrays.toString(request));
                                    if (request != null) {
                                        switch (request[1]) {
                                            case 1:
                                                //REQUEST-Message

                                                String bookHash1 = player.getLibrary().getHashes().get(request[2]);
                                                System.out.println(bookHash1);
                                                Book book = player.getStuff().getBooks().get(bookHash1);
                                                System.out.println("book size: " + book.getSize());
                                                System.out.println("requested:= " + request[2]);
                                                //send the PIECE-Message = 4
                                                byte[] pieceMessageHead = new byte[3];
                                                pieceMessageHead[0] = (byte) (pieceMessageHead.length + book.getSize() - 1);
                                                System.out.println("pieceMessageHead Size:= " + ((byte) (pieceMessageHead.length + book.getSize() - 1)));
                                                pieceMessageHead[1] = (byte) 4;
                                                pieceMessageHead[2] = request[2];
                                                byte[] pieceMessage = ArrayUtils.addAll(pieceMessageHead, book.getBook());
                                                System.out.println("Sending peace message: " + Arrays.toString(pieceMessage));
                                                NetUtils.sendBytes(socket.getOutputStream(), pieceMessage);
//                                                socket.getOutputStream().write(pieceMessage);
                                                break;
                                            case 3:
                                                System.out.println("nb players " + player.getPlayers().size());
                                                String[] socketInfo = socket.getRemoteSocketAddress().toString().substring(1).split(":");
                                                String bookHash = player.getLibrary().getHashes().get(request[2]);
                                                System.out.println("socketInfo: " + Arrays.toString(socketInfo));
                                                String playerHash = player.getHashByIp(socketInfo[0]);
                                                System.out.println("playerHash: " + playerHash);
                                                System.out.println("bookHash: " + bookHash);
                                                Player player1 = player.getPlayers().get(playerHash);
                                                System.out.println("player1 is null?:== " + (player1 == null));
                                                System.out.println("player1.getStuff() == " + (player1.getStuff() == null));
                                                player1.getStuff().getBooks().get(bookHash);
                                                break;
                                            default:
                                                System.out.println("this is not my concern");
                                                break;
                                        }
                                        System.out.println("request: " + request);
                                    } else {
                                        return;
                                    }
                                }

                            }
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }
}
