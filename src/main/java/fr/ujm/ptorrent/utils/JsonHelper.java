package fr.ujm.ptorrent.utils;

import fr.ujm.ptorrent.model.Library;
import fr.ujm.ptorrent.model.Player;
import fr.ujm.ptorrent.model.Stuff;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class JsonHelper {


    public void createLibraryFile(Stuff stuff,List<String> sha1Boocks) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ip", NetUtils.getCurrentIp().getHostAddress());
        jsonObject.put("port", "9999");

        jsonObject.put("version", 1);
        jsonObject.put("file", stuff.getName());
        jsonObject.put("size", stuff.getFile().length());
        jsonObject.put("piece", Stuff.SIZE);
        JSONArray jsonArrayPiecesHash = new JSONArray();
        System.out.println(stuff.getBooks().size());
        for (int i = 0; i < sha1Boocks.size() ; i++) {
            System.out.println("pice: " + sha1Boocks.get(i));
            jsonArrayPiecesHash.put(sha1Boocks.get(i));

        }
        jsonObject.put("hashes", jsonArrayPiecesHash);

        String json = jsonObject.toString();
        File file = new File(JsonHelper.class.getClassLoader().getResource("libs").getPath() + "/" + stuff.getName() + ".lib");
        if (!file.exists())
            file.createNewFile();

        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(json);
        fileWriter.close();
        System.out.println("\nJSON Object: " + json);

    }

    public Library readLibraryFile(String libName) throws IOException {
        String jsonString = new String(Files.readAllBytes(Paths.get(JsonHelper.class.getClassLoader().getResource("libs").getPath() + "/" + libName + ".lib")));
        JSONObject lib = new JSONObject(jsonString);
        Library library = new Library();
        library.setFile(lib.get("file").toString());
        library.setIp(lib.get("ip").toString());
        library.setPort(lib.get("port").toString());
        library.setSize(lib.get("size").toString());
        library.setPieceSize(lib.get("piece").toString());
        library.setVersion(lib.get("version").toString());
        JSONArray hashesJson = new JSONArray(lib.get("hashes").toString());
        List<String> hashes = new ArrayList<>();
        for (int i = 0; i < hashesJson.length(); i++) {
            hashes.add(hashesJson.get(i).toString());
        }
        library.setHashes(hashes);

        return library;

    }

    public String jsonEncodePlayerTOHub(Player player) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("peer_id", player.getId());
        jsonObject.put("ip", player.getIpAddress().getHostAddress());
        jsonObject.put("port", player.getPort());
        jsonObject.put("left", player.getStuffleftSize());
        jsonObject.put("event", "NONE");

        return jsonObject.toString();
    }

    public Player jsonDecodePlayerTOHub(String jsonPlayer) throws UnknownHostException {
        Player player = new Player();
        JSONObject jsonObject = new JSONObject(jsonPlayer);
        player.setId(jsonObject.get("peer_id").toString());
        player.setIpAddress(InetAddress.getByName(jsonObject.get("ip").toString()));
        player.setPort(Integer.parseInt(jsonObject.get("port").toString()));
        jsonObject.put("left", 123);
        jsonObject.put("event", "NONE");

        return player;
    }

    public Map<String, Player> decodeHubPlayerList(String jsonPlayers, Player owner) throws UnknownHostException {
        Map<String, Player> playersMap = new HashMap<>();
        JSONObject jsonObject = new JSONObject(jsonPlayers);
        JSONArray jsonArray = new JSONArray(jsonObject.get("peers").toString());
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonPlayer = new JSONObject(jsonArray.get(i).toString());
            Player player = new Player();
            player.setPort(Integer.parseInt(jsonPlayer.get("peer_port").toString()));
            player.setIpAddress(InetAddress.getByName(jsonPlayer.get("peer_ip").toString()));
            player.setId(jsonPlayer.get("peer_id").toString());
            playersMap.put(player.getId(), player);
        }
        owner.setRequestInterval(Integer.parseInt(jsonObject.get("interval").toString()));
        return playersMap;
    }

    public String encodeHubPlayerList(BlockingQueue<Player> players, Player current) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonPlayerList = new JSONArray();
        jsonObject.put("interval", "60");
        for (Player player : players) {
            System.out.println("current: " + current.getId());
            System.out.println("player: " + player.getId());
            if (!current.getId().equals(player.getId())) {
                JSONObject jsonPlayer = new JSONObject();
                jsonPlayer.put("peer_id", player.getId());
                jsonPlayer.put("peer_ip", player.getIpAddress().getHostAddress());
                jsonPlayer.put("peer_port", player.getPort());
                jsonPlayerList.put(jsonPlayer);
            }
        }
        jsonObject.put("peers", jsonPlayerList);
        return jsonObject.toString();
    }


}
