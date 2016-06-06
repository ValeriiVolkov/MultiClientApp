package msitse.chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static msitse.chat.ChatUtils.*;

/**
 * Created by Volkov Valerii
 */
public class ChatSocketServer {
    private Socket socket = null;
    private Map<String, Socket> socketList;
    private List<String> clientIpList;

    private int port;

    public ChatSocketServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Parameter for port is needed");
        } else {
            try {
                ChatSocketServer chatServer = new ChatSocketServer(Integer.valueOf(args[0]));
                chatServer.createSocket();
            } catch (NumberFormatException e) {
                System.out.println("Incorrect input for the port");
            }
        }
    }

    /**
     * Creates socket
     */
    private void createSocket() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            socketList = new HashMap<String, Socket>();
            clientIpList = new ArrayList<String>();
            System.out.println("Server is started");
            System.out.println("Input " + EXIT + " to exit from app (no strict rules for capitalization)");
            System.out.println("Press ENTER key before typing the message to a client");
            while (true) {
                socket = serverSocket.accept();
                clientIpList.add(socket.getInetAddress().getHostAddress());
                showMessageAboutOperations();
                socketList.put(socket.getRemoteSocketAddress().toString(), socket);
                createReadThread();
                createWriteThread();
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    /**
     * Creates read thread for getting messages from clients
     */
    private void createReadThread() {
        ClientReadThread clientReadThread = new ClientReadThread(socket, this);
        clientReadThread.setPriority(Thread.MAX_PRIORITY);
        clientReadThread.start();
    }

    private void createWriteThread() {
        Thread writeThread = new Thread() {
            public void run() {
                try {
                    while (socket.isConnected()) {
                        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
                        sleep(SLEEP_TIME);
                        String typedMessage = inputReader.readLine();
                        handleExitMessage(typedMessage);
                        handleMessageShowAllClients(typedMessage);
                        handleMessageKickOut(typedMessage);
                        if (typedMessage != null && typedMessage.length() > 0) {
                            sendToAllConnectedClients(typedMessage);
                            sleep(SLEEP_TIME);
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    interrupt();
                }
            }
        };
        writeThread.setPriority(Thread.MAX_PRIORITY);
        writeThread.start();
    }

    /**
     * Handles message for client's exit. Show ip as identificator of a client
     *
     * @param socket
     */
    private void handleClientExit(Socket socket) {
        //In the case when client exit the application without inputting needed message
        String address = socket.getInetAddress().getHostAddress();
        String message = address + " : " + ServiceMessages.CLIENT_QUITED_THE_CHAT.message();
        clientIpList.remove(address);
        socketList.remove(address);
        System.out.println(message);
        handleMessageShowAllClients(message);
    }

    /**
     * Shows the message about the input for showing all connected clients when
     * the first client is connected
     */
    private void showMessageAboutOperations() {
        if (clientIpList.size() == 1) {
            System.out.println("Input " + SHOW_ALL_CLIENTS + " to show all connected clients " +
                    "(no strict rules for capitalization)");
            System.out.println("Enter KICK_<IP> to kick out user from chat");
        }
    }

    /**
     * Removes socket with given IP from the list of connected sockets
     *
     * @param ip
     */
    private void removeSocketWithIP(String ip) throws IOException {
        for (Map.Entry<String, Socket> entry : socketList.entrySet()) {
            Socket s = entry.getValue();
            if (s.getInetAddress().getHostAddress().contains(ip)) {
                s.close();
                socketList.remove(s.getRemoteSocketAddress().toString());
                return;
            }
        }
    }

    /**
     * Returns the list of all connected clients
     */
    public List<String> getClients() {
        return clientIpList;
    }

    /**
     * Closes a socket
     *
     * @param socket
     * @throws IOException
     */
    public void closeClient(Socket socket) throws IOException {
        handleClientExit(socket);
        socket.close();
    }

    /**
     * Sends messages to all clients except a client who send the message
     *
     * @param message
     */
    public void sendToAllConnectedClientsExceptCurrent(String message, String currentAdress) throws IOException {
        for (Map.Entry<String, Socket> entry : socketList.entrySet()) {
            String address = entry.getKey();
            OutputStream outputStream = entry.getValue().getOutputStream();
            try {
                if (!address.equals(currentAdress)) {
                    outputStream.write(message.getBytes(CHARSET));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles message to show all connected clients
     *
     * @param message
     */
    private void handleMessageShowAllClients(String message) {
        if (message.toUpperCase().equals(SHOW_ALL_CLIENTS)) {
            System.out.println("The following clients are connected:");
            int i = 1;
            for (String clientIp : clientIpList) {
                System.out.println(i + " : " + clientIp);
            }
        }
    }

    /**
     * Handles message to kick client from chat
     *
     * @param message
     */
    private void handleMessageKickOut(String message) throws IOException {
        if (message.toUpperCase().contains(KICK)) {
            String ip = message.split("_")[1];
            String kickOutMessage = ServiceMessages.KICK.message() + ip;
            if (clientIpList.contains(ip)) {
                sendToAllConnectedClients(kickOutMessage);
                removeSocketWithIP(ip);
                clientIpList.remove(ip);
                System.out.println(kickOutMessage);
            } else {
                System.out.println("The input for IP is incorrect");
            }
        }
    }

    /**
     * Handles message about exit from a server
     *
     * @param message
     */
    private void handleExitMessage(String message) throws IOException {
        if (message.toUpperCase().equals(EXIT)) {
            sendToAllConnectedClients(SERVER_EXIT);
            System.exit(0);
        }
    }

    /**
     * Sends messages to all clients
     *
     * @param message
     */
    private void sendToAllConnectedClients(String message) throws IOException {
        for (Map.Entry<String, Socket> entry : socketList.entrySet()) {
            OutputStream outputStream = entry.getValue().getOutputStream();
            try {
                outputStream.write(wrapWithIP(message).getBytes(CHARSET));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
