package server;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Builds every message the server sends. Format: TYPE|data
 *
 *   WELCOME|nickname                  your (new) nickname
 *   SYSTEM|text                       server notice
 *   ERROR|text                        something went wrong
 *   CHAT|nickname|time|text           chat message
 *   USERS|nick1,nick2                 online users
 *   VOTES|map1=2,map2=0               vote counts
 *   READINESS|nick1=true,nick2=false  ready status
 *   HELP|text                         command list
 */
public class Protocol {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a");

    public static String system(String message) {
        return "SYSTEM|" + clean(message);
    }

    public static String error(String message) {
        return "ERROR|" + clean(message);
    }

    public static String welcome(String nickname) {
        return "WELCOME|" + clean(nickname);
    }

    public static String chat(String nickname, String message) {
        String time = LocalTime.now().format(TIME_FORMAT);

        return "CHAT|" + clean(nickname)
                + "|" + time
                + "|" + clean(message);
    }

    public static String users(Collection<String> names) {
        return "USERS|" + String.join(",", names);
    }

    public static String voteResults(
            Set<String> maps, Map<String, String> votes) {

        Map<String, Integer> results = new TreeMap<>();

        for (String map : maps) {
            results.put(map, 0);
        }

        for (String vote : votes.values()) {
            results.put(vote, results.getOrDefault(vote, 0) + 1);
        }

        StringBuilder result = new StringBuilder("VOTES|");
        boolean first = true;

        for (Map.Entry<String, Integer> entry : results.entrySet()) {
            if (!first) {
                result.append(",");
            }
            first = false;

            result.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
        }

        return result.toString();
    }

    public static String readiness(Map<String, Boolean> readiness) {

        StringBuilder result = new StringBuilder("READINESS|");
        boolean first = true;

        for (Map.Entry<String, Boolean> entry : readiness.entrySet()) {
            if (!first) {
                result.append(",");
            }
            first = false;

            result.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
        }

        return result.toString();
    }

    public static String help() {
        return "HELP|Commands: /name <newname>, /vote <map>, "
                + "/ready, /notready, /help, /exit";
    }

    public static boolean isValidNickname(String nickname) {
        return nickname != null
                && nickname.matches("[A-Za-z0-9_]{3,15}");
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("|", "/")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
