package fr.ujm.ptorrent.model;

import lombok.Data;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

@Data
public class Stuff {
    public static final int SIZE = 20000;
    private String name;
    private File file;
    private Map<String, Book> books = new HashMap<>();
    private int left;

    public Stuff() {
    }

    public Stuff(String pathName) throws FileNotFoundException {
        System.out.println(pathName);
        this.file = new File(pathName);
        System.out.println(file.isDirectory());
        if (!file.exists())
            throw new FileNotFoundException();
        this.name = file.getName();
    }



}
