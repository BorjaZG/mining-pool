package server;

import common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MiningGuiServer extends JFrame {
    // Componentes UI
    private JTextArea logArea;
    private JLabel statusLabel;

    // Lógica del Servidor
    public static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public static volatile boolean solved = false;

    // --- NUEVO: LISTA DE TRANSACCIONES PENDIENTES ---
    // Usamos CopyOnWriteArrayList para que sea thread-safe
    public static final List<String> pendingTransactions = new CopyOnWriteArrayList<>();

    // Instancia estática
    private static MiningGuiServer instance;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MiningGuiServer().setVisible(true);
        });
    }

    public MiningGuiServer() {
        instance = this;

        // Configuración Ventana
        setTitle("Mining Pool SERVER - Panel de Control");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel de Logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 255, 0));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Barra de Estado
        statusLabel = new JLabel(" Estado: Esperando transacciones...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(statusLabel, BorderLayout.SOUTH);

        startServerBackground();
    }

    private void startServerBackground() {
        new Thread(() -> {
            int port = Protocol.DEFAULT_PORT;
            log("--- INICIANDO SERVIDOR EN PUERTO " + port + " ---");

            // Iniciar gestor de bloques (ahora esperará transacciones)
            new BlockManager().start();

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                log("Servidor listo y escuchando.");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    log(">>> NUEVA CONEXIÓN: " + clientSocket.getInetAddress());

                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    handler.start();

                    updateClientCount();
                }
            } catch (IOException e) {
                log("Error crítico en servidor: " + e.getMessage());
            }
        }).start();
    }

    // --- NUEVO MÉTODO: AÑADIR TRANSACCIÓN ---
    public static void addTransaction(String tx) {
        pendingTransactions.add(tx);
        log("--> Nueva transacción recibida: " + tx);

        // Actualizar UI
        if (instance != null) {
            SwingUtilities.invokeLater(() ->
                    instance.statusLabel.setText(" Acumulando transacciones: " + pendingTransactions.size() + "/3")
            );
        }
    }

    // Métodos de utilidad
    public static void log(String msg) {
        if (instance != null) {
            SwingUtilities.invokeLater(() -> {
                instance.logArea.append(msg + "\n");
                instance.logArea.setCaretPosition(instance.logArea.getDocument().getLength());
            });
        } else {
            System.out.println(msg);
        }
    }

    public static void updateClientCount() {
        // Solo actualizamos si no estamos en medio de una carga de transacciones
        if (instance != null && pendingTransactions.isEmpty()) {
            SwingUtilities.invokeLater(() ->
                    instance.statusLabel.setText(" Clientes conectados: " + clients.size())
            );
        }
    }

    public static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public static void notifySolutionFound(String winnerInfo) {
        if (!solved) {
            solved = true;
            log("!!! GANADOR: " + winnerInfo + " !!!");
            log("--- Bloque cerrado. Notificando a todos ---");
            broadcast(Protocol.RESP_END);
        }
    }
}