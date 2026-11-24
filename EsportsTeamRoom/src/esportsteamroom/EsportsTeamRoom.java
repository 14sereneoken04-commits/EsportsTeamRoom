
package esportsteamroom;

import javax.swing.SwingUtilities;
import GUI.LoginFrame;

public class EsportsTeamRoom {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });

    }
}
