package fr.ujm.ptorrent.model;

import lombok.Data;

import java.util.List;

@Data
public class Library {
    private String ip;
    private String port;
    private String version;
    private String file;
    private String size;
    private String pieceSize;
    private List<String> hashes;

    @Override
    public String toString() {
        return  file;
    }
}
