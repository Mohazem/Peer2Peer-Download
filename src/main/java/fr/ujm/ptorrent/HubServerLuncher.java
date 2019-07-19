package fr.ujm.ptorrent;


import fr.ujm.ptorrent.server.HubServer;

import java.io.File;
import java.io.IOException;

public class HubServerLuncher {
    public static void main(String[] args) throws IOException {
        HubServer hubServer = new HubServer();
        hubServer.start();

    }
}
