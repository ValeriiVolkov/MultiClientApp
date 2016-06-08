package msitse.chat;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static msitse.chat.ChatUtils.*;

/**
 * Created by Volkov Valerii
 */
public class ChatSocketClient {
    private Socket socket = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;

    private String nameOfContainer;
    private int port;

    private Logger LOGGER = Logger.getLogger("ChatSocketClient");

    public ChatSocketClient(String nameOfContainer, int port) {
        this.nameOfContainer = nameOfContainer;
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Two parameters: 1st - for ip, 2nd - for port are needed");
        } else {
            try {
                System.out.println("Input " + EXIT + " to exit from app (no strict rules for capitalization)");
                ChatSocketClient myChatClient = new ChatSocketClient(args[0], Integer.valueOf(args[1]));
                showIp();
                myChatClient.createSocket();
            } catch (NumberFormatException e) {
                System.out.println("Wrong input for the port");
            }
        }
    }

    /**
     * Creates socket
     */
    public void createSocket() {
        try {
            System.out.println("Connection...");
            socket = new Socket(nameOfContainer, port);
            System.out.println("Connection established");
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
            createReadThread();
            createWriteThread();

            sendMessageConnectionEstablished(outStream);
        } catch (UnknownHostException ue) {
            LOGGER.log(Level.SEVERE, "Unknown Host Exception", ue);
        } catch (IOException io) {
            LOGGER.log(Level.SEVERE, "IO Exception", io);
        }
    }

    /**
     * Creates read thread for getting messages from server
     */
    public void createReadThread() {
        Thread readThread = new Thread() {
            public void run() {
                while (socket.isConnected()) {
                    try {
                        byte[] readBuffer = new byte[SIZE_OF_BUFFER];
                        int size = inStream.read(readBuffer);
                        if (size > 0) {
                            byte[] arrayBytes = new byte[size];
                            System.arraycopy(readBuffer, 0, arrayBytes, 0, size);
                            String receivedMessage = new String(arrayBytes, ChatUtils.CHARSET);

                            System.out.println(RECEIVED_FROM + receivedMessage);
                        }
                    } catch (SocketException se) {
                        LOGGER.log(Level.SEVERE, "Socket Exception", se);
                        System.exit(0);
                    } catch (IOException ie) {
                        LOGGER.log(Level.SEVERE, "IO Exception", ie);
                    }
                }
            }
        };
        readThread.setPriority(Thread.MAX_PRIORITY);
        readThread.start();
    }

    /**
     * Creates write thread for sending messages to server
     */
    public void createWriteThread() {
        Thread writeThread = new Thread() {
            public void run() {
                while (socket.isConnected()) {
                    try {
                        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
                        sleep(SLEEP_TIME);
                        String inputMessage = inputReader.readLine();
                        handleExitMessage(inputMessage, outStream);
                        if (inputMessage != null && inputMessage.length() > 0) {
                            synchronized (socket) {
                                outStream.write(wrapWithIP(inputMessage).getBytes(CHARSET));
                                sleep(SLEEP_TIME);
                            }
                        }
                    } catch (IOException ie) {
                        LOGGER.log(Level.SEVERE, "IO Exception", ie);
                    } catch (InterruptedException ie) {
                        LOGGER.log(Level.SEVERE, "Interrupted Exception", ie);
                    }
                }
            }
        };
        writeThread.setPriority(Thread.MAX_PRIORITY);
        writeThread.start();
    }

    /**
     * Sends message about established connection to server
     *
     * @param outStream
     */
    private void sendMessageConnectionEstablished(OutputStream outStream) {
        try {
            outStream.write(wrapWithIP(ServiceMessages.CONNECTION_ESTABLISHED.message()).getBytes(CHARSET));
        } catch (IOException ie) {
            LOGGER.log(Level.SEVERE, "IO Exception", ie);
        }
    }

    /**
     * Handles message about exit from a client
     *
     * @param message
     * @param outStream
     */
    private void handleExitMessage(String message, OutputStream outStream) {
        if (message.toUpperCase().equals(EXIT)) {
            try {
                outStream.write(wrapWithIP(ServiceMessages.CLIENT_QUITED_THE_CHAT.message()).getBytes(CHARSET));
                System.exit(0);
            } catch (IOException ie) {
                LOGGER.log(Level.SEVERE, "IO Exception", ie);
            }
        }
    }
}
