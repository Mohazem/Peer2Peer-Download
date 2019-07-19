package fr.ujm.ptorrent.utils;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * @author
 */
public class NetUtils {

    public static final String LINE_SEPARATOR = "\r\n";

    public static String readLine(InputStream inputStream) throws IOException {

        ByteArrayOutputStream res = new ByteArrayOutputStream();

        boolean justSeenR = false;
        while (true) {
            int valueRead = inputStream.read();

            if (valueRead == -1) {
                return null;
            }

            byte b = (byte) valueRead;

            if (b == '\n' && justSeenR) {
                break;
            }
            if (justSeenR) {
                res.write('\r');
            }
            if (b == '\r') {
                justSeenR = true;
            } else {
                justSeenR = false;
                res.write(b);
            }
        }
        return res.toString("utf-8");
    }

    public static byte[] readBytes(InputStream inputStream) throws IOException, ClassNotFoundException {
        DataInputStream dIn = new DataInputStream(inputStream);

        int length = dIn.readInt();                    // read length of incoming message
        System.out.println(length);
        if (length > 0) {
            byte[] message = new byte[length];
            dIn.readFully(message, 0, message.length); // read the message
            return message;
        }
        return null;

    }

    public static void sendBytes(OutputStream outputStream, byte[] message) throws IOException {
        DataOutputStream dOut = new DataOutputStream(outputStream);
        dOut.writeInt(message.length); // write length of the message
        dOut.write(message);

    }

    public static InetAddress getCurrentIp() throws SocketException, UnknownHostException {
        InetAddress ip;
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress();
        }
        if ("0.0.0.0".equals(ip.getHostAddress())) {
            ip = InetAddress.getLocalHost();
        }

        return ip;
    }

}