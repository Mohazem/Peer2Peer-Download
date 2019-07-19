package fr.ujm.ptorrent.utils;

import fr.ujm.ptorrent.model.Book;
import fr.ujm.ptorrent.model.Stuff;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Librairifier {

    // inspired by: http://www.mkyong.com/java/how-to-generate-a-file-checksum-value-in-java/
    public static String bytesToSHA1(byte[] bytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            byte[] digestedBytes = messageDigest.digest(bytes);

            // convert the digestedBytes to hexadecimal format to reduce the size of the ouput
            StringBuilder result = new StringBuilder();
            for (byte digestedByte : digestedBytes) {
                result.append(Integer.toString((digestedByte & 0xff) + 0x100, 16).substring(1));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] readFileToByteArray(File file) {

        byte[] bArray = new byte[(int) file.length()];
        try {
            FileInputStream fis = new FileInputStream(file);
            fis.read(bArray);
            fis.close();

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
        }
        return bArray;
    }

    public void stuffToBooks(Stuff stuff) {
        int size = Stuff.SIZE;
        List<String> sha1Boocks = new ArrayList<>();
        byte[] bArray = readFileToByteArray(stuff.getFile());
        String directoryName = Librairifier.class.getClassLoader().getResource("files").getPath() + "/" + stuff.getName() + "/chunks";
        File directory = new File(directoryName);
        if (!directory.exists())
            directory.mkdirs();
        for (int i = 0; i < bArray.length; i = i + size) {
            if (size < bArray.length - i) {
                byte[] bytes = Arrays.copyOfRange(bArray, i, i + size);
                stuff.getBooks().put(bytesToSHA1(bytes), new Book(bytes, size, i));
                sha1Boocks.add(bytesToSHA1(bytes));
                File file = createFile(directoryName + "/" + bytesToSHA1(bytes) + "_" + i / size);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(Arrays.copyOfRange(bArray, i, i + size));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                byte[] bytes = Arrays.copyOfRange(bArray, i, bArray.length);
                stuff.getBooks().put(bytesToSHA1(bytes), new Book(bytes, bArray.length - i, i));
                sha1Boocks.add(bytesToSHA1(bytes));
                File file = createFile(directoryName + "/" + bytesToSHA1(bytes) + "_" + i / size);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(Arrays.copyOfRange(bArray, i, bArray.length));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
        JsonHelper jsonHelper = new JsonHelper();
        try {
            jsonHelper.createLibraryFile(stuff, sha1Boocks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveBook(Book book, String stuffName) {

        String directoryName = Librairifier.class.getClassLoader().getResource("files").getPath() + "/" + stuffName + "/chunks/";
        File file = createFile(directoryName + bytesToSHA1(book.getBook()) + "_" + book.getPos());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(book.getBook());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void booksToStuff(Map<String, Book> books, String fileName) {
        List<Book> bookList = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            for (String key : books.keySet()) {
                if (books.get(key).getPos() == i)
                    bookList.add(books.get(key));
            }
        }
        File file = createFile(Librairifier.class.getClassLoader().getResource("files").getPath() + "/" + fileName + "/" + fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // fos.close();
            for (Book book : bookList) {
                fos.write(book.getBook());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    File createFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                if (!file.createNewFile())
                    throw new FileNotFoundException();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;

    }


}
