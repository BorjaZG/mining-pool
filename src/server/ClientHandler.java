package server;

import common.HashUtils;
import common.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String currentPayload; // Para validar la solución si este cliente la envía

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // Procesar mensajes del cliente
                if (inputLine.startsWith(Protocol.CMD_CONNECT)) {
                    // Fuente [33]: "ack - X total clients"
                    int count = MiningServer.clients.size();
                    sendMessage(Protocol.RESP_ACK + " " + count + " total clients");
                    System.out.println("Cliente registrado. Total: " + count);

                } else if (inputLine.startsWith(Protocol.CMD_SOL)) {
                    // Fuente [41]: "client1: sol 36"
                    handleSolution(inputLine);
                }
            }
        } catch (IOException e) {
            System.err.println("Cliente desconectado abruptamente.");
        } finally {
            disconnect();
        }
    }

    // Envía un mensaje a este cliente específico
    public void sendMessage(String msg) {
        out.println(msg);
    }

    // Guarda el payload actual para poder validar luego
    public void setCurrentPayload(String payload) {
        this.currentPayload = payload;
    }

    private void handleSolution(String input) {
        // Formato esperado: "sol <numero>"
        try {
            String[] parts = input.split(" ");
            String saltStr = parts[1];

            // VALIDACIÓN DE LA SOLUCIÓN (Obligatorio)
            // Fuente [64]: "El servidor deberá validar la solución aportada"
            if (currentPayload != null) {
                String toCheck = currentPayload + saltStr;
                String hash = HashUtils.getSha256(toCheck);

                // Comprobamos si realmente empieza por "00000" (o lo que definas)
                // Para pruebas rápidas, usa 4 o 5 ceros. El PDF menciona 2 ceros en la pág 3,
                // pero eso es muy fácil, mejor probar con 4 o 5.
                if (HashUtils.checkHash(hash, 4)) {
                    MiningServer.notifySolutionFound("Cliente " + this.getId());
                } else {
                    System.out.println("Solución incorrecta recibida del cliente " + this.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando solución: " + e.getMessage());
        }
    }

    private void disconnect() {
        try {
            MiningServer.clients.remove(this);
            socket.close();
            System.out.println("Cliente desconectado. Restantes: " + MiningServer.clients.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}