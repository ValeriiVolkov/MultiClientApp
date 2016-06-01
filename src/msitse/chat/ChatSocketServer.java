package msitse.chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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
    private InputStream inStream = null;
    private OutputStream currentOutStream = null;
    private HashMap<String, OutputStream> outStreamList;
    private List<String> clientIpList;
    private String currentSocketAdress;

    private int port;

/*    public ChatSocketServer(int port) {
        this.port = port;
    }*/

    public static void main(String[] args) {
        ChatSocketServer chatServer = new ChatSocketServer();
        chatServer.createSocket();

        /*if(args.length < 1)
        {
            System.out.println("Parameter for port is needed");
        }
        else{*/
        /*try {
            //msitse.chat.ChatSocketClient myChatClient = new msitse.chat.ChatSocketClient(args[0], Integer.valueOf(args[1]));
            ChatSocketServer chatServer = new ChatSocketServer(Integer.valueOf(args[0]));
            chatServer.createSocket();
        }
        catch (NumberFormatException e)
        {
            System.out.println("Wrong input for the port");
        }*/
        //}
    }

    /**
     * Creates socket
     */
    public void createSocket() {
        try {
            ServerSocket serverSocket = new ServerSocket(8000);
            outStreamList = new HashMap<String, OutputStream>();
            clientIpList = new ArrayList<String>();
            System.out.println("Server is started");
            while (true) {
                socket = serverSocket.accept();
                clientIpList.add(socket.getInetAddress().getHostAddress());
                showMessageAboutInputForClientsList();

                inStream = socket.getInputStream();
                currentOutStream = socket.getOutputStream();
                outStreamList.put(socket.getInetAddress().getHostAddress(), currentOutStream);

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
    public void createReadThread() {
        Thread readThread = new Thread() {
            public void run() {
                while (socket.isConnected()) {
                    try {
                        byte[] readBuffer = new byte[200];
                        int num = inStream.read(readBuffer);
                        if (num > 0) {
                            byte[] arrayBytes = new byte[num];
                            System.arraycopy(readBuffer, 0, arrayBytes, 0, num);
                            String message = new String(arrayBytes, CHARSET);
                            System.out.println(RECEIVED_FROM + message);
                            currentSocketAdress = socket.getInetAddress().getHostAddress();
                            sendToAllConnectedClients(wrapWithIP(message));
                        } else {
                            handleClientExit(socket);
                            //If there is at least one connected client then notify these clients
                            if(!clientIpList.isEmpty()) {
                                notify();
                            }
                        }
                    } catch (SocketException se) {
                        System.exit(0);
                    } catch (IOException i) {
                        i.printStackTrace();
                    } catch (IllegalMonitorStateException ie) {
                        //Catch to exclude message about exception
                        /*Do nothing*/
                    }
                }
            }
        };
        readThread.setPriority(Thread.MAX_PRIORITY);
        readThread.start();
    }

    /**
     * Creates write thread for sending messages to clients
     */
    public void createWriteThread() {
        Thread writeThread = new Thread() {
            public void run() {
                try {
                    while (socket.isConnected()) {
                        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
                        sleep(SLEEP_TIME);
                        String typedMessage = inputReader.readLine();
                        handleMessageShowAllClients(typedMessage);
                        if (typedMessage != null && typedMessage.length() > 0) {
                            synchronized (socket) {
                                currentOutStream.write(typedMessage.getBytes(CHARSET));
                                sleep(SLEEP_TIME);
                            }
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
                }
            }
        };
        writeThread.setPriority(Thread.MAX_PRIORITY);
        writeThread.start();
    }

    /**
     * Sends messages to all clients except a client who send the message
     *
     * @param message
     */
    private void sendToAllConnectedClients(String message) {
        for (Map.Entry<String, OutputStream> entry : outStreamList.entrySet()) {
            String address = entry.getKey();
            OutputStream outputStream = entry.getValue();
            try {
                if (!address.equals(currentSocketAdress)) {
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
     * Handles message for client's exit. Show ip as identificator of a client
     *
     * @param socket
     */
    private void handleClientExit(Socket socket) {
        //In the case when client exit the application without inputting needed message
        String address = socket.getInetAddress().getHostAddress();
        String message = address + " : " + ServiceMessages.CLIENT_QUITED_THE_CHAT.toString();
        clientIpList.remove(address);
        outStreamList.remove(address);
        System.out.println(message);
        handleMessageShowAllClients(message);
    }


    /**
     * Shows the message about the input for showing all connected clients when
     * the first client is connected
     */
    private void showMessageAboutInputForClientsList() {
        if (clientIpList.size() == 1) {
            System.out.println("Input " + SHOW_ALL_CLIENTS + " to show all connected clients " +
                    "(no strict rules for capitalization)");
        }
    }
}
