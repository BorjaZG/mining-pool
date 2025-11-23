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

    // Lógica del Servidor (Variables estáticas para acceso global)
    // Fuente [61]: Lista concurrente de conexiones
    public static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public static volatile boolean solved = false;

    // Instancia estática para permitir loguear desde otras clases
    private static MiningGuiServer instance;

    public static void main(String[] args) {
        // Iniciar la UI en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> {
            new MiningGuiServer().setVisible(true);
        });
    }

    public MiningGuiServer() {
        instance = this;

        // Configuración de la Ventana
        setTitle("Mining Pool SERVER - Panel de Control");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel de Logs (Estilo Consola)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30)); // Gris oscuro
        logArea.setForeground(new Color(0, 255, 0));  // Texto verde estilo terminal
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Barra de Estado Inferior
        statusLabel = new JLabel(" Estado: Iniciando...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(statusLabel, BorderLayout.SOUTH);

        // Arrancar el servidor en un hilo aparte para no congelar la ventana
        startServerBackground();
    }

    private void startServerBackground() {
        new Thread(() -> {
            int port = Protocol.DEFAULT_PORT;
            log("--- INICIANDO SERVIDOR EN PUERTO " + port + " ---");

            // Iniciar el gestor de bloques (hilo que genera trabajo)
            // Fuente [25, 59]: Generar bloques periódicamente
            new BlockManager().start();

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                SwingUtilities.invokeLater(() -> statusLabel.setText(" Estado: ESCUCHANDO en puerto " + port));

                while (true) {
                    // Fuente [62]: Gestionar conexiones de manera concurrente
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

    // --- MÉTODOS ESTÁTICOS PARA USO DE OTRAS CLASES ---

    // Escribir en el área de texto de la ventana
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

    // Actualizar el contador de clientes en la barra inferior
    public static void updateClientCount() {
        if (instance != null) {
            SwingUtilities.invokeLater(() ->
                    instance.statusLabel.setText(" Estado: ACTIVO | Clientes conectados: " + clients.size())
            );
        }
    }

    // Enviar mensaje a todos (Broadcast)
    public static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // Notificar victoria
    public static void notifySolutionFound(String winnerInfo) {
        if (!solved) {
            solved = true;
            log("!!! GANADOR: " + winnerInfo + " !!!");
            log("--- Bloque cerrado. Notificando fin a todos ---");
            // Fuente [51, 64]: Validar y finalizar proceso en el resto
            broadcast(Protocol.RESP_END);
        }
    }
}