package msitse.chat;

import java.net.Inet4Address;
import java.net.UnknownHostException;

/**
 * Created by Volkov Valerii
 */
public class ChatUtils {
    public static final int SIZE_OF_BUFFER = 200;
    public static final String CHARSET = "UTF-8";
    public static int SLEEP_TIME = 80;

    public static final String RECEIVED_FROM = "Received from ";
    public static final String SERVER_EXIT = "Server is not available";

    public static final String SHOW_ALL_CLIENTS = "SHOW_CLIENTS";
    public static final String KICK = "KICK_";
    public static final String EXIT = "EXIT";

    /**
     * Wraps the message with ip. Needed to differ messages from clients
     *
     * @param message
     * @return
     * @throws UnknownHostException
     */
    public static String wrapWithIP(String message) throws UnknownHostException {
        return Inet4Address.getLocalHost().getHostAddress() + " : " + message;
    }

    /**
     * Outputs the ip of a client
     * @throws UnknownHostException
     */
    public static void showIp() throws UnknownHostException {
        System.out.println("IP of client: " + Inet4Address.getLocalHost().getHostAddress());
    }
}
