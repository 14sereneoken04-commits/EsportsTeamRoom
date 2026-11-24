package GUI;

import client.ChatClient;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

    


/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author Acer
 */
public class ChatRoomFrame extends javax.swing.JFrame {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ChatRoomFrame.class.getName());
    private ChatClient chatClient;
    private String nickname = "";

    private static final String PLACEHOLDER = "Type your message...";
    private final DefaultListModel<String> playerModel = new DefaultListModel<>();
    private final List<String> users = new ArrayList<>();
    private final Map<String, Boolean> readyMap = new HashMap<>();
    private boolean mapsLoaded = false;

    public ChatRoomFrame(ChatClient chatClient, String nickname) {
        this();
        this.chatClient = chatClient;
        this.nickname = nickname;

        chatClient.setMessageListener(msg ->
                SwingUtilities.invokeLater(() -> handleServerMessage(msg)));
    }

    public ChatRoomFrame() {
        initComponents();
        setupRoom();
    }

    private void setupRoom() {
        setTitle("Esports Team Room");
        listPlayers.setModel(playerModel);
        txtMessages.setLineWrap(true);
        txtMessages.setWrapStyleWord(true);
         txtMessage.setEditable(true);
        txtMessage.setEnabled(true);
        txtVotes.setEditable(false);
        cmbMaps.removeAllItems();
        getRootPane().setDefaultButton(jButton1);

        txtMessage.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtMessage.getText().equals(PLACEHOLDER)) {
                    txtMessage.setText("");
                }
            }
        });

        btnVote.addActionListener(e -> voteSelected());

        btnReady.addActionListener(e -> {
            if (chatClient != null) {
                chatClient.setReady(btnReady.isSelected());
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (chatClient != null) {
                    chatClient.disconnect();
                }
            }
        });
    }

    private void appendLine(String text) {
        txtMessages.append(text + "\n");
        txtMessages.setCaretPosition(txtMessages.getDocument().getLength());
    }

    private void lockControls() {
        jButton1.setEnabled(false);
        txtMessage.setEnabled(false);
        btnVote.setEnabled(false);
        btnReady.setEnabled(false);
        cmbMaps.setEnabled(false);
        appendLine("! You are disconnected. Close this window and log in again.");
    }

    private void handleServerMessage(String raw) {

        String[] p = raw.split("\\|", 2);
        String type = p[0];
        String data = p.length > 1 ? p[1] : "";

        switch (type) {

            case "WELCOME":
                nickname = data;
                setTitle("Esports Team Room - " + nickname);
                appendLine("* You are connected as " + nickname
                        + ". Type /help for commands.");
                break;

            case "CHAT": {
                String[] c = data.split("\\|", 3);
                if (c.length == 3) {
                    appendLine("[" + c[1] + "] " + c[0] + ": " + c[2]);
                }
                break;
            }

            case "SYSTEM":
                appendLine("* " + data);
                break;

            case "HELP":
                appendLine("* " + data);
                break;

            case "ERROR":
                appendLine("! " + data);
                if (!chatClient.isConnected()) {
                    lockControls();
                }
                break;

            case "USERS":
                users.clear();
                if (!data.isEmpty()) {
                    users.addAll(Arrays.asList(data.split(",")));
                }
                refreshPlayers();
                break;

            case "READINESS":
                readyMap.clear();
                if (!data.isEmpty()) {
                    for (String entry : data.split(",")) {
                        String[] kv = entry.split("=");
                        if (kv.length == 2) {
                            readyMap.put(kv[0], Boolean.parseBoolean(kv[1]));
                        }
                    }
                }
                refreshPlayers();
                break;

            case "VOTES":
                showVotes(data);
                break;

            default:
                appendLine(raw);
        }
    }

    private void refreshPlayers() {
        playerModel.clear();
        int ready = 0;

        for (String u : users) {
            boolean r = readyMap.getOrDefault(u, false);
            if (r) {
                ready++;
            }
            playerModel.addElement((r ? "[READY]  " : "[waiting]  ") + u);
        }

        lblConnectedPlayers.setText("Players (" + ready + "/" + users.size() + " ready)");

        boolean me = readyMap.getOrDefault(nickname, false);
        btnReady.setSelected(me);
        btnReady.setText(me ? "I'M READY" : "NOT READY");
    }

    private void showVotes(String data) {
        StringBuilder sb = new StringBuilder("MAP VOTES\n");
        String leader = null;
        int best = 0;

        for (String entry : data.split(",")) {
            String[] kv = entry.split("=");
            if (kv.length != 2) {
                continue;
            }

            if (!mapsLoaded) {
                cmbMaps.addItem(kv[0]);
            }

            int count = Integer.parseInt(kv[1]);
            sb.append(kv[0]).append(": ").append(count).append("\n");

            if (count > best) {
                best = count;
                leader = kv[0];
            }
        }
        mapsLoaded = true;

        sb.append(leader == null ? "No votes yet" : "Leading: " + leader);
        txtVotes.setText(sb.toString());
    }

    private void voteSelected() {
        Object sel = cmbMaps.getSelectedItem();
        if (chatClient != null && chatClient.isConnected() && sel != null) {
            chatClient.voteForMap(sel.toString());
        }
    }

    private void handleCommand(String text) {
        String[] parts = text.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {
            case "/name":
                if (arg.isEmpty()) {
                    appendLine("! Usage: /name <newname>");
                } else {
                    chatClient.changeNickname(arg);
                }
                break;

            case "/vote":
                if (arg.isEmpty()) {
                    appendLine("! Usage: /vote <map>");
                } else {
                    chatClient.voteForMap(arg);
                }
                break;

            case "/ready":
                chatClient.setReady(true);
                break;

            case "/notready":
                chatClient.setReady(false);
                break;

            case "/help":
                chatClient.requestHelp();
                break;

            case "/exit":
                chatClient.disconnect();
                dispose();
                new LoginFrame().setVisible(true);
                break;

            default:
                appendLine("! Unknown command. Type /help.");
        }
    }
  

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlHeader = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel2 = new javax.swing.JPanel();
        lblConnectedPlayers = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listPlayers = new javax.swing.JList<>();
        cmbMaps = new javax.swing.JComboBox<>();
        btnVote = new javax.swing.JButton();
        btnReady = new javax.swing.JToggleButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtVotes = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        txtMessage = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtMessages = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        pnlHeader.setBackground(new java.awt.Color(0, 0, 0));
        pnlHeader.setForeground(new java.awt.Color(255, 255, 255));

        lblTitle.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        lblTitle.setForeground(new java.awt.Color(0, 204, 204));
        lblTitle.setText("ESPORTS TEAM ROOM");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("TEAM ISUFST CHAT ROOM");

        javax.swing.GroupLayout pnlHeaderLayout = new javax.swing.GroupLayout(pnlHeader);
        pnlHeader.setLayout(pnlHeaderLayout);
        pnlHeaderLayout.setHorizontalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addGap(334, 334, 334)
                .addComponent(lblTitle)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlHeaderLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(341, 341, 341))
        );
        pnlHeaderLayout.setVerticalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addComponent(lblTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addContainerGap(32, Short.MAX_VALUE))
        );

        jSeparator1.setForeground(new java.awt.Color(0, 0, 0));

        jPanel2.setBackground(new java.awt.Color(0, 0, 51));

        lblConnectedPlayers.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblConnectedPlayers.setForeground(new java.awt.Color(255, 255, 255));
        lblConnectedPlayers.setText("Connected Players");

        listPlayers.setBackground(new java.awt.Color(153, 153, 153));
        listPlayers.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jScrollPane1.setViewportView(listPlayers);

        btnVote.setText("VOTE MAP");
        btnVote.addActionListener(this::btnVoteActionPerformed);

        btnReady.setText("NOT READY");
        btnReady.addActionListener(this::btnReadyActionPerformed);

        txtVotes.setEditable(false);
        txtVotes.setColumns(20);
        txtVotes.setRows(5);
        jScrollPane3.setViewportView(txtVotes);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                    .addComponent(cmbMaps, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnVote, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnReady, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane3)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(lblConnectedPlayers, javax.swing.GroupLayout.PREFERRED_SIZE, 258, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblConnectedPlayers, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmbMaps, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnVote, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnReady)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
        );

        jPanel3.setBackground(new java.awt.Color(0, 0, 51));
        jPanel3.setForeground(new java.awt.Color(0, 0, 51));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 255, 255));
        jLabel9.setText("Messages");

        txtMessage.setEditable(false);
        txtMessage.setText("Type your message...");

        jButton1.setBackground(new java.awt.Color(0, 153, 204));
        jButton1.setText("send");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        txtMessages.setEditable(false);
        txtMessages.setBackground(new java.awt.Color(204, 204, 204));
        txtMessages.setColumns(20);
        txtMessages.setRows(5);
        jScrollPane2.setViewportView(txtMessages);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jScrollPane2)
                        .addContainerGap())
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(txtMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 464, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(29, 29, 29))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addGap(18, 18, 18))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnlHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnlHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 386, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        

           String text = txtMessage.getText().trim();

        if (text.isEmpty() || text.equals(PLACEHOLDER)) {
            appendLine("! Blank messages are not allowed.");
            return;
        }

        if (chatClient == null || !chatClient.isConnected()) {
            JOptionPane.showMessageDialog(this,
                    "You are not connected to the server!");
            return;
        }

        if (text.startsWith("/")) {
            handleCommand(text);
        } else {
            chatClient.sendMessage(text);
        }

        txtMessage.setText("");

    }//GEN-LAST:event_jButton1ActionPerformed

    private void btnReadyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReadyActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnReadyActionPerformed

    private void btnVoteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVoteActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnVoteActionPerformed
            
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new ChatRoomFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton btnReady;
    private javax.swing.JButton btnVote;
    private javax.swing.JComboBox<String> cmbMaps;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lblConnectedPlayers;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JList<String> listPlayers;
    private javax.swing.JPanel pnlHeader;
    private javax.swing.JTextField txtMessage;
    private javax.swing.JTextArea txtMessages;
    private javax.swing.JTextArea txtVotes;
    // End of variables declaration//GEN-END:variables
}