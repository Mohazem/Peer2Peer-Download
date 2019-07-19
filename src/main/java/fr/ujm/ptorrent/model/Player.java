package fr.ujm.ptorrent.model;

import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;

@Data
public class Player {
    private String id;
    private Map<String, Player> players = new HashMap<>();
    private InetAddress ipAddress;
    private int port;
    private int requestInterval = 100;
    //    private ArrayList<Book> books;
    private Stuff stuff = new Stuff();
    private Library library;
    private Socket hubSocket;

    public Player() {
    }

    public Player(InetAddress ipAddress, int port) {
        id = UUID.randomUUID().toString().replace("-", "");
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public void loadFilesFromHardrive() {
        try {
            System.out.println("loading from hardrive ......");
            if (library == null)
                return;
            String path = Objects.requireNonNull(Player.class.getClassLoader().getResource("files")).getPath() + "/" + library.getFile() + "/chunks";
            File fileStuff = new File(path);
            if (!fileStuff.exists()) {
                fileStuff.mkdirs();
            }
            Map<String, Book> bookMap = new HashMap<>();
            for (final File file : new File(path).listFiles()) {
                //            library.getHashes().
                if (!file.getName().contains("_"))
                    continue;
                String[] fileInfo = file.getName().split("_");
                if (fileInfo.length != 2)
                    continue;
                System.out.println("part[ " + fileInfo[1] + "] ...");
                if (library.getHashes().contains(fileInfo[0])) {
                    int size = Integer.parseInt(library.getPieceSize());
                    byte[] data = Files.readAllBytes(file.toPath());
                    Book book = new Book(data, size, Integer.parseInt(fileInfo[1]));
                    bookMap.put(fileInfo[0], book);
                }
            }
            this.stuff.setBooks(bookMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Done!");

    }

    public String getHashByIp(String ip) {
        for (String key : players.keySet()) {
            System.out.println("player list keys: " + key);
            System.out.println(players.get(key).getIpAddress().getHostAddress());
            if (players.get(key).getIpAddress().getHostAddress().equals(ip))
                return key;
        }

        return "-1";
    }

    public int getStuffleftSize() {
        int size = Integer.parseInt(this.library.getSize());
        for (String key : this.stuff.getBooks().keySet()) {
            size -= this.stuff.getBooks().get(key).getSize();
        }
        return size < 0 ? 0 : size;
    }

    public boolean stuffIsDownloaded() {

        if (stuff.getBooks().keySet().size() != library.getHashes().size())
            return false;
        System.out.println(Arrays.toString(stuff.getBooks().keySet().toArray()));
        System.out.println(Arrays.toString(library.getHashes().toArray()));
        for (String key : stuff.getBooks().keySet()) {
            boolean check = false;
            for (String val : library.getHashes()) {
                if (key.equals(val))
                    check = true;
            }
            if (!check)
                return false;
        }
        return true;

    }

    public boolean isConnetedToHub() {
        return hubSocket != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(getId(), player.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
