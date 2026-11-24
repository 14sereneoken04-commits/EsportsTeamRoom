package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;

    private BufferedReader input;
    private PrintWriter output;

    private volatile String nickname;
    private volatile boolean exiting = false;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public synchronized void send(String message) {
        if (output != null) {
            output.println(message);
        }
    }

    @Override
    public void run() {

        boolean registered = false;

        try {
            input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            output = new PrintWriter(socket.getOutputStream(), true);

            // First message must be JOIN|nickname
            String firstMessage = input.readLine();

            if (firstMessage == null) {
                return;
            }

            String[] joinData = firstMessage.split("\\|", 2);

            if (joinData.length != 2 || !joinData[0].equals("JOIN")) {
                send(Protocol.error("Invalid connection request."));
                return;
            }

            registered = server.registerClient(this, joinData[1].trim());

            if (!registered) {
                return;
            }

            String message;

            while ((message = input.readLine()) != null) {
                handleMessage(message);
            }

        } catch (IOException e) {

            if (!exiting) {
                System.out.println("Connection lost: " + nickname);
            }

        } finally {

            if (registered) {
                server.removeClient(this);
            }

            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing connection.");
            }
        }
    }

    private void handleMessage(String message) {

        if (message == null || message.trim().isEmpty()) {
            send(Protocol.error("Blank messages are not allowed."));
            return;
        }

        String[] parts = message.split("\\|", 2);

        String command = parts[0];

        String data = parts.length > 1 ? parts[1].trim() : "";

        switch (command) {

            case "CHAT":
                if (!data.isEmpty()) {
                    server.broadcastMessage(nickname, data);
                } else {
                    send(Protocol.error("Message cannot be empty."));
                }
                break;

            case "NAME":
                server.renameClient(this, data);
                break;

            case "VOTE":
                server.voteForMap(nickname, data);
                break;

            case "READY":
                if (data.equalsIgnoreCase("true")) {
                    server.updateReadiness(nickname, true);
                } else if (data.equalsIgnoreCase("false")) {
                    server.updateReadiness(nickname, false);
                } else {
                    send(Protocol.error("Invalid readiness value."));
                }
                break;

            case "HELP":
                send(Protocol.help());
                break;

            case "EXIT":
                exiting = true;
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error disconnecting.");
                }
                break;

            default:
                send(Protocol.error("Unknown command. Type /help for commands."));
                break;
        }
    }
}
