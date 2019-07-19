package fr.ujm.ptorrent.model;

import lombok.Data;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Data
public class Hub {

    private BlockingQueue<Player> connectedPlayersList;

    public Hub() {
        this.connectedPlayersList = new LinkedBlockingQueue<>();
    }


    public void addPlayer(Player player) throws InterruptedException {
        connectedPlayersList.put(player);
    }
}
