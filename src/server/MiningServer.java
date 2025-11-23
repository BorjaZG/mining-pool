package server;

import common.Protocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MiningServer {
    // Lista segura para hilos (concurrente) para guardar los clientes
    // Fuente [61]: "El servidor deberá llevar una lista de las conexiones actuales"
    public static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // Variables para gestionar el minado actual
    public static volatile boolean solved = false; // Bandera para saber si ya se resolvió el bloque actual

    public static void main(String[] args) {
        int port = Protocol.DEFAULT_PORT;
        System.out.println("--- MINING POOL SERVER INICIADO en puerto " + port + " ---");

        // Iniciamos el gestor de bloques (el que genera trabajo cada X tiempo)
        // Fuente [25]: "El mining pool generará un nuevo 'bloque' cada 5 minutos"
        new BlockManager().start();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                // Esperar a que llegue un minero
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado: " + clientSocket.getInetAddress());

                // Crear un hilo dedicado para este cliente
                // Fuente [62]: "El servidor gestionará de manera concurrente las conexiones"
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para enviar el mismo mensaje a TODOS los clientes (Broadcast)
    public static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // Método para notificar que alguien encontró la solución
    public static void notifySolutionFound(String winnerInfo) {
        if (!solved) {
            solved = true;
            System.out.println("!!! SOLUCIÓN ENCONTRADA POR " + winnerInfo + " !!!");
            // Fuente [51, 64]: "end: un cliente ha encontrado la solución... finalizará el proceso"
            broadcast(Protocol.RESP_END);
        }
    }
}