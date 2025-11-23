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
    // Componentes de la UI
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JButton connectButton;
    private JLabel statusLabel;

    // Variables de red
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private MiningWorker currentWorker;

    public static void main(String[] args) {
        // Iniciar la ventana en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> new MinerGuiClient().setVisible(true));
    }

    public MinerGuiClient() {
        // Configuración de la Ventana
        setTitle("Miner Client - Mining Pool");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. Panel Superior (Botón Conectar)
        JPanel topPanel = new JPanel();
        connectButton = new JButton("Conectar al Servidor");
        connectButton.addActionListener(e -> connectToServer());
        topPanel.add(connectButton);
        add(topPanel, BorderLayout.NORTH);

        // 2. Panel Central (Logs)
        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // 3. Panel Inferior (Barra de Progreso y Estado)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        statusLabel = new JLabel(" Estado: Desconectado");

        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                log("Conectando a " + Protocol.DEFAULT_HOST + "...");
                socket = new Socket(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Enviar comando inicial
                out.println(Protocol.CMD_CONNECT);

                SwingUtilities.invokeLater(() -> {
                    connectButton.setEnabled(false);
                    statusLabel.setText(" Estado: Conectado y esperando trabajo...");
                });

                // Bucle de escucha (Listener)
                String serverMsg;
                while ((serverMsg = in.readLine()) != null) {
                    processMessage(serverMsg);
                }

            } catch (IOException e) {
                log("Error de conexión: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    connectButton.setEnabled(true);
                    statusLabel.setText(" Estado: Error de conexión");
                });
            }
        }).start();
    }

    private void processMessage(String msg) {
        log("Servidor: " + msg);

        if (msg.startsWith(Protocol.RESP_NEW_REQUEST)) {
            stopWorker();
            try {
                // Parsear mensaje: new_request 0-100000 mv|...
                String[] parts = msg.split(" ");
                String[] range = parts[1].split("-");
                String payload = parts[2];
                long start = Long.parseLong(range[0]);
                long end = Long.parseLong(range[1]);

                log(">>> ¡NUEVO TRABAJO! Rango: " + start + " - " + end);
                out.println(Protocol.RESP_ACK);

                // Actualizar UI
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(0);
                    statusLabel.setText(" Estado: MINANDO (" + start + "-" + end + ")");
                });

                // Iniciar Worker
                currentWorker = new MiningWorker(start, end, payload, this);
                currentWorker.start();

            } catch (Exception e) {
                log("Error procesando trabajo: " + e.getMessage());
            }
        }
        else if (msg.startsWith(Protocol.RESP_END)) {
            stopWorker();
            log(">>> FIN. Bloque resuelto por otro.");
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

    // Método llamado por el Worker para actualizar la barra
    public void updateProgress(int percent) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
    }

    public synchronized void sendSolution(long salt) {
        log("!!! SOLUCIÓN ENCONTRADA: " + salt + " !!!");
        out.println(Protocol.CMD_SOL + " " + salt);
        SwingUtilities.invokeLater(() -> statusLabel.setText(" Estado: ¡GANADOR! Esperando confirmación..."));
    }

    private void log(String text) {
        SwingUtilities.invokeLater(() -> logArea.append(text + "\n"));
    }
}