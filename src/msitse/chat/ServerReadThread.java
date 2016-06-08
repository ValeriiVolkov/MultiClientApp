package msitse.chat;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static msitse.chat.ChatUtils.CHARSET;
import static msitse.chat.ChatUtils.RECEIVED_FROM;

/**
 * Created by Valerii Volkov
 */
public class ServerReadThread extends Thread {
    private Socket socket;
    private InputStream inputStream;
    private ChatSocketServer server;
    private String currentSocketAdress;

    private Logger LOGGER = Logger.getLogger("ServerReadThread");

    public ServerReadThread(Socket socket, ChatSocketServer server)
    {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        while (socket.isConnected()) {
            try {
                byte[] readBuffer = new byte[200];

                inputStream = socket.getInputStream();
                int size = inputStream.read(readBuffer);
                if (size > 0) {
                    byte[] arrayBytes = new byte[size];
                    System.arraycopy(readBuffer, 0, arrayBytes, 0, size);
                    String message = new String(arrayBytes, CHARSET);
                    System.out.println(RECEIVED_FROM + message);

                    synchronized (socket) {
                        currentSocketAdress = socket.getRemoteSocketAddress().toString();
                        server.sendToAllConnectedClientsExceptCurrent(message, currentSocketAdress);
                    }
                } else {
                    //If there is at least one connected client then notify these clients
                    if (!server.getClients().isEmpty()) {
                        notify();
                        server.closeClient(socket);
                    }
                }
            } catch (SocketException se) {
                //Catched, however, not logged to eliminate excess output in the console
            } catch (IOException i) {
                LOGGER.log(Level.SEVERE, "IO Exception", i);
            } catch (IllegalMonitorStateException ie) {
                LOGGER.log(Level.SEVERE, "Illegal Monitor State Exception", ie);
            } finally {
                interrupt();
            }
        }
    }
}
