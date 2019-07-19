package fr.ujm.ptorrent.server;

import fr.ujm.ptorrent.model.Hub;
import fr.ujm.ptorrent.model.Player;
import fr.ujm.ptorrent.utils.JsonHelper;
import fr.ujm.ptorrent.utils.NetUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.out;

public class HubServer extends Thread {
    private final Logger LOGGER = Logger.getLogger(HubServer.class.getName());
    private BlockingQueue<Player> connectedPlayersList;

    public void run() {
        final Hub hub = new Hub();
        connectedPlayersList = hub.getConnectedPlayersList();

        LOGGER.log(Level.INFO, "Starting the hub server ....");
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(9999, -1, NetUtils.getCurrentIp());

            System.out.println(serverSocket.getInetAddress().getHostAddress());
            // process clients one by one (not in parallel)
            boolean timerExecuted = false;
            while (true) {
                if (!timerExecuted) {
                    CleaningThread cleaningThread = CleaningThread.getInstance(connectedPlayersList);
                    Timer timer = new Timer();
                    timer.schedule(cleaningThread, 0, 10000);
                    timerExecuted = true;
                }
                //scoket used by the player to connect to the hub
                Socket playerSocket = serverSocket.accept();
                out.println("ACCEPTING CONNECTIONS FROM" + playerSocket.getRemoteSocketAddress());
                ReadWriteThread readWriteThread = new ReadWriteThread(playerSocket, connectedPlayersList, hub);
                readWriteThread.start();


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

class ReadWriteThread extends Thread {
    String jsonPlayer = "";
    Socket playerSocket;
    BlockingQueue<Player> connectedPlayersList;
    Hub hub;

    public ReadWriteThread(Socket playerSocket, BlockingQueue<Player> connectedPlayersList, Hub hub) {
        this.playerSocket = playerSocket;
        this.connectedPlayersList = connectedPlayersList;
        this.hub = hub;
    }

    @Override
    public void run() {
        while (true) {
            try {
//        synchronized (this) {

//                synchronized (this) {
                out.println("reading data from player .......");
                jsonPlayer = NetUtils.readLine(playerSocket.getInputStream());
                out.println("RECEIVED FIRSTxx: " + jsonPlayer);
                if (jsonPlayer != null) {
                    final JsonHelper jsonHelper = new JsonHelper();
                    Player player = jsonHelper.jsonDecodePlayerTOHub(jsonPlayer);
                    if (!connectedPlayersList.contains(player)) {
                        player.setHubSocket(playerSocket);
                        connectedPlayersList.put(player);
                    }

                    String jsonPlayers = jsonHelper.encodeHubPlayerList(connectedPlayersList, player) + "\r\n";
                    System.out.println("sending: " + jsonPlayers);
                    playerSocket.getOutputStream().write(jsonPlayers.getBytes("utf-8"));
                } else {
                    playerSocket.close();
                    return;
                }
//                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class CleaningThread extends TimerTask {
    private static CleaningThread cleaningThread = null;
    private BlockingQueue<Player> connectedPlayersList;

    private CleaningThread(BlockingQueue<Player> socketList) {
        this.connectedPlayersList = socketList;
    }

    public static CleaningThread getInstance(BlockingQueue<Player> socketLis) {
        if (cleaningThread == null)
            cleaningThread = new CleaningThread(socketLis);

        return cleaningThread;
    }

    public void run() {
        try {
//            System.out.println("**** Im in refresh block **** ");
//            System.out.println("List size: " + connectedPlayersList.size());

            for (Player player : connectedPlayersList) {
//                System.out.println(player);
//                System.out.println("Should I remove this player: [[ " + player.getId() + " ]]" + (player.getHubSocket().isClosed()));
                if (player.getHubSocket().isClosed()) {
                    connectedPlayersList.remove(player);
//                    System.out.println("the dead man is removed :D");
                }
//                System.out.println("**** Im out of refresh block **** ");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
//        }
}

