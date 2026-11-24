package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class ChatClient {

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private volatile boolean connected = false;
    private volatile String lastError = "";

    private Consumer<String> messageListener = null;

    // Messages that arrive before the GUI has registered its listener
    // (e.g. USERS / VOTES right after joining) are kept here.
    private final List<String> pending = new ArrayList<>();

    public synchronized void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;

        for (String message : pending) {
            listener.accept(message);
        }
        pending.clear();
    }

    private synchronized void deliver(String message) {
        if (message == null) {
            return;
        }

        if (messageListener == null) {
            pending.add(message);
        } else {
            messageListener.accept(message);
        }
    }

    public String getLastError() {
        return lastError;
    }

    // Connect to the server
    public boolean connect(String ip, int port, String nickname) {

        lastError = "";

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 4000);

            // only wait a few seconds for the JOIN reply
            socket.setSoTimeout(5000);

            input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            output = new PrintWriter(socket.getOutputStream(), true);

            output.println("JOIN|" + nickname);

            String response = input.readLine();

            if (response == null) {
                lastError = "The server closed the connection.";
                disconnect();
                return false;
            }

            if (response.startsWith("ERROR|")) {
                lastError = response.substring(6);
                disconnect();
                return false;
            }

            socket.setSoTimeout(0);   // no timeout while chatting
            connected = true;

            deliver(response);        // WELCOME|nickname

            Thread readerThread = new Thread(this::readLoop);
            readerThread.setDaemon(true);
            readerThread.start();

            return true;

        } catch (IOException e) {
            lastError = "Cannot reach " + ip + ":" + port
                    + " (" + e.getMessage() + ")";
            disconnect();
            return false;
        }
    }

    private void readLoop() {
        try {
            String message;

            while (connected && (message = input.readLine()) != null) {
                deliver(message);
            }

        } catch (IOException e) {
            // handled in finally
        } finally {
            boolean wasConnected = connected;
            connected = false;
            closeQuietly();

            if (wasConnected) {
                deliver("ERROR|Connection to the server was lost.");
            }
        }
    }

    // Send a chat message
    public void sendMessage(String message) {
        if (connected && message != null && !message.trim().isEmpty()) {
            output.println("CHAT|" + message.trim());
        }
    }

    // Change nickname
    public void changeNickname(String newName) {
        if (connected) {
            output.println("NAME|" + newName);
        }
    }

    // Vote for a map
    public void voteForMap(String map) {
        if (connected) {
            output.println("VOTE|" + map);
        }
    }

    // Update team readiness
    public void setReady(boolean ready) {
        if (connected) {
            output.println("READY|" + ready);
        }
    }

    // Request help
    public void requestHelp() {
        if (connected) {
            output.println("HELP");
        }
    }

    // Disconnect from server
    public void disconnect() {

        if (connected && output != null) {
            output.println("EXIT");
        }

        connected = false;
        closeQuietly();
    }

    private void closeQuietly() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing socket.");
        }
    }

    // Check connection
    public boolean isConnected() {
        return connected;
    }

    // Test client using console
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        ChatClient client = new ChatClient();
        client.setMessageListener(System.out::println);

        System.out.println("================================");
        System.out.println("       ESPORTS TEAM ROOM");
        System.out.println("================================");

        System.out.print("Enter nickname: ");
        String nickname = scanner.nextLine();

        if (!client.connect("127.0.0.1", 5000, nickname)) {
            System.out.println(client.getLastError());
            scanner.close();
            return;
        }

        System.out.println("\nCommands:");
        System.out.println("/vote Kochab");
        System.out.println("/ready");
        System.out.println("/notready");
        System.out.println("/name NewName");
        System.out.println("/help");
        System.out.println("/exit");

        while (client.isConnected()) {

            String message = scanner.nextLine();

            if (message.equalsIgnoreCase("/exit")) {
                client.disconnect();
                break;

            } else if (message.startsWith("/vote ")) {
                client.voteForMap(message.substring(6));

            } else if (message.equalsIgnoreCase("/ready")) {
                client.setReady(true);

            } else if (message.equalsIgnoreCase("/notready")) {
                client.setReady(false);

            } else if (message.startsWith("/name ")) {
                client.changeNickname(message.substring(6));

            } else if (message.equalsIgnoreCase("/help")) {
                client.requestHelp();

            } else {
                client.sendMessage(message);
            }
        }

        scanner.close();
        System.out.println("Disconnected from server.");
    }
}
