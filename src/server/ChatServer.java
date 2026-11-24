package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    private static final int PORT = 5000;

    private ServerSocket serverSocket;

    private final Map<String, ClientHandler> clients =
            new ConcurrentHashMap<>();

    private final Map<String, String> votes =
            new ConcurrentHashMap<>();

    private final Map<String, Boolean> readiness =
            new ConcurrentHashMap<>();

    // Change these names if you want different maps.
    private static final Set<String> MAPS = Set.of(
            "HEALING TURLE", "REVEALING WISPS", "GOLDEN TURRET", "FLYING CLOUDS"
    );

    public static void main(String[] args) {
        new ChatServer().start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);

            String ip = "unknown";
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (IOException ignored) {
                // keep "unknown"
            }

            System.out.println("================================");
            System.out.println("       ESPORTS TEAM ROOM");
            System.out.println("================================");
            System.out.println("Server running on port " + PORT);
            System.out.println("Server IP (check ipconfig if wrong): " + ip);
            System.out.println("Waiting for players...");

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();

                System.out.println("New connection: "
                        + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket, this);

                Thread thread = new Thread(handler);
                thread.start();
            }

        } catch (IOException e) {
            if (serverSocket == null || !serverSocket.isClosed()) {
                System.out.println("Server error: " + e.getMessage());
            }
        }
    }

    // ---------- helpers ----------

    private boolean isTaken(String nickname, ClientHandler except) {
        for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
            if (e.getKey().equalsIgnoreCase(nickname)
                    && e.getValue() != except) {
                return true;
            }
        }
        return false;
    }

    // ---------- join / rename / leave ----------

    public synchronized boolean registerClient(
            ClientHandler client, String nickname) {

        if (!Protocol.isValidNickname(nickname)) {
            client.send(Protocol.error(
                    "Nickname must be 3-15 characters using letters, numbers or underscores."));
            return false;
        }

        if (isTaken(nickname, client)) {
            client.send(Protocol.error("Nickname is already taken!"));
            return false;
        }

        client.setNickname(nickname);
        clients.put(nickname, client);
        readiness.put(nickname, false);

        client.send(Protocol.welcome(nickname));

        broadcast(Protocol.system(nickname + " joined the team room."));

        sendOnlineUsers();
        sendVoteResults();
        sendReadiness();

        System.out.println(nickname + " joined.");

        return true;
    }

    public synchronized boolean renameClient(
            ClientHandler client, String newName) {

        String oldName = client.getNickname();

        if (!Protocol.isValidNickname(newName)) {
            client.send(Protocol.error(
                    "Invalid nickname. Use 3-15 letters, numbers or underscores."));
            return false;
        }

        if (oldName.equals(newName)) {
            client.send(Protocol.error("That is already your nickname."));
            return false;
        }

        if (isTaken(newName, client)) {
            client.send(Protocol.error("That nickname is already taken."));
            return false;
        }

        clients.remove(oldName);
        clients.put(newName, client);

        client.setNickname(newName);

        String oldVote = votes.remove(oldName);
        if (oldVote != null) {
            votes.put(newName, oldVote);
        }

        Boolean oldReady = readiness.remove(oldName);
        readiness.put(newName, oldReady != null ? oldReady : false);

        // tell this client its new nickname
        client.send(Protocol.welcome(newName));

        broadcast(Protocol.system(oldName + " changed nickname to " + newName));

        sendOnlineUsers();
        sendVoteResults();
        sendReadiness();

        return true;
    }

    public synchronized void removeClient(ClientHandler client) {

        String nickname = client.getNickname();

        if (nickname == null) {
            return;
        }

        if (clients.remove(nickname, client)) {

            votes.remove(nickname);
            readiness.remove(nickname);

            broadcast(Protocol.system(nickname + " left the team room."));

            sendOnlineUsers();
            sendVoteResults();
            sendReadiness();

            System.out.println(nickname + " disconnected.");
        }
    }

    // ---------- chat ----------

    public void broadcastMessage(String nickname, String message) {
        broadcast(Protocol.chat(nickname, message));
    }

    // ---------- original feature 1: map voting ----------

    public synchronized void voteForMap(String nickname, String mapName) {

        ClientHandler client = clients.get(nickname);

        if (client == null) {
            return;
        }

        String selectedMap = null;

        for (String map : MAPS) {
            if (map.equalsIgnoreCase(mapName)) {
                selectedMap = map;
                break;
            }
        }

        if (selectedMap == null) {
            client.send(Protocol.error("Invalid map. Choose one of: "
                    + String.join(", ", new java.util.TreeSet<>(MAPS))));
            return;
        }

        votes.put(nickname, selectedMap);

        broadcast(Protocol.system(nickname + " voted for " + selectedMap));

        sendVoteResults();
    }

    // ---------- original feature 2: ready check ----------

    public synchronized void updateReadiness(String nickname, boolean ready) {

        if (!readiness.containsKey(nickname)) {
            return;
        }

        readiness.put(nickname, ready);

        broadcast(Protocol.system(
                nickname + (ready ? " is READY!" : " is NOT READY.")));

        sendReadiness();
    }

    // ---------- broadcasting ----------

    private void sendOnlineUsers() {
        broadcast(Protocol.users(clients.keySet()));
    }

    private void sendVoteResults() {
        broadcast(Protocol.voteResults(MAPS, votes));
    }

    private void sendReadiness() {
        broadcast(Protocol.readiness(readiness));
    }

    private void broadcast(String message) {
        for (ClientHandler client : clients.values()) {
            client.send(message);
        }
    }
}
