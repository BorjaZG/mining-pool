package client;

import common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MinerGuiClient extends JFrame {
    // Componentes UI
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JButton connectButton;
    private JLabel statusLabel;

    // --- NUEVOS COMPONENTES PARA TRANSACCIONES ---
    private JTextField destField;
    private JTextField amountField;
    // ---------------------------------------------

    // Red y Worker
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private MiningWorker currentWorker;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MinerGuiClient().setVisible(true));
    }

    public MinerGuiClient() {
        setTitle("Miner Client - Mining Pool");
        setSize(500, 500); // Un poco más alta para que quepan los campos
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- PANEL SUPERIOR (Conexión + Enviar Dinero) ---
        JPanel topPanel = new JPanel(new GridLayout(2, 1));

        // 1. Panel Conexión
        JPanel connectPanel = new JPanel();
        connectButton = new JButton("Conectar al Servidor");
        connectButton.addActionListener(e -> connectToServer());
        connectPanel.add(connectButton);
        topPanel.add(connectPanel);

        // 2. Panel Transacciones (NUEVO)
        JPanel txPanel = new JPanel();
        txPanel.setBorder(BorderFactory.createTitledBorder("Enviar Operación"));

        destField = new JTextField(8);
        amountField = new JTextField(5);
        JButton sendButton = new JButton("Enviar");

        txPanel.add(new JLabel("Destino:"));
        txPanel.add(destField);
        txPanel.add(new JLabel("€:"));
        txPanel.add(amountField);
        txPanel.add(sendButton);

        // Acción del botón
        sendButton.addActionListener(e -> sendTransaction());

        topPanel.add(txPanel);
        add(topPanel, BorderLayout.NORTH);
        // -------------------------------------------------

        // Panel Central (Logs)
        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Panel Inferior (Progreso)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        statusLabel = new JLabel(" Estado: Desconectado");

        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // --- NUEVO MÉTODO DE ENVÍO ---
    private void sendTransaction() {
        if (out != null) {
            String dest = destField.getText();
            String cant = amountField.getText();

            if (!dest.isEmpty() && !cant.isEmpty()) {
                // Envía: tx DESTINO CANTIDAD
                out.println(Protocol.CMD_SEND_TX + " " + dest + " " + cant);
                log("-> Enviada operación a " + dest + " (" + cant + "€)");

                // Limpiar campos
                destField.setText("");
                amountField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Rellena destino y cantidad");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Primero conéctate al servidor");
        }
    }
    // -----------------------------

    private void connectToServer() {
        new Thread(() -> {
            try {
                log("Conectando...");
                socket = new Socket(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println(Protocol.CMD_CONNECT);

                SwingUtilities.invokeLater(() -> {
                    connectButton.setEnabled(false);
                    statusLabel.setText(" Estado: Conectado. Esperando trabajo...");
                });

                String serverMsg;
                while ((serverMsg = in.readLine()) != null) {
                    processMessage(serverMsg);
                }

            } catch (IOException e) {
                log("Error: " + e.getMessage());
                SwingUtilities.invokeLater(() -> connectButton.setEnabled(true));
            }
        }).start();
    }

    private void processMessage(String msg) {
        log("Servidor: " + msg);

        if (msg.startsWith(Protocol.RESP_NEW_REQUEST)) {
            stopWorker();
            try {
                // CORRECCIÓN: Usamos límite 3 para no romper el payload si tiene espacios
                String[] parts = msg.split(" ", 3);

                String[] range = parts[1].split("-");
                String payload = parts[2]; // Ahora cogerá "mv|...pepe 50;..." entero

                long start = Long.parseLong(range[0]);
                long end = Long.parseLong(range[1]);

                log(">>> ¡NUEVO BLOQUE! Minando...");
                out.println(Protocol.RESP_ACK);

                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(0);
                    statusLabel.setText(" Estado: MINANDO (" + start + "-" + end + ")");
                });

                currentWorker = new MiningWorker(start, end, payload, this);
                currentWorker.start();

            } catch (Exception e) {
                log("Error parsing request: " + e.getMessage());
            }
        }
        else if (msg.startsWith(Protocol.RESP_END)) {
            stopWorker();
            log(">>> FIN DEL BLOQUE.");
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(0);
                statusLabel.setText(" Estado: Esperando siguiente bloque...");
            });
        }
    }

    public void stopWorker() {
        if (currentWorker != null && currentWorker.isAlive()) {
            currentWorker.interrupt();
        }
    }

    public void updateProgress(int percent) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
    }

    public synchronized void sendSolution(long salt) {
        log("!!! SOLUCIÓN ENCONTRADA: " + salt + " !!!");
        out.println(Protocol.CMD_SOL + " " + salt);
        SwingUtilities.invokeLater(() -> statusLabel.setText(" Estado: ¡GANADOR!"));
    }

    private void log(String text) {
        SwingUtilities.invokeLater(() -> logArea.append(text + "\n"));
    }
}