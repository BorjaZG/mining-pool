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
    private String currentPayload;

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

                if (inputLine.startsWith(Protocol.CMD_CONNECT)) {
                    // Fuente [33]: Responder con ACK y total de clientes
                    int count = MiningGuiServer.clients.size();
                    sendMessage(Protocol.RESP_ACK + " " + count + " total clients");

                    // Log en la ventana del servidor
                    MiningGuiServer.log("Cliente registrado (Hilo ID: " + this.getId() + ")");
                    MiningGuiServer.updateClientCount();

                } else if (inputLine.startsWith(Protocol.CMD_SOL)) {
                    // Fuente [63]: Recibir solución
                    handleSolution(inputLine);
                }
            }
        } catch (IOException e) {
            // Cliente cerrado
        } finally {
            disconnect();
        }
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public void setCurrentPayload(String payload) {
        this.currentPayload = payload;
    }

    private void handleSolution(String input) {
        try {
            String[] parts = input.split(" ");
            String saltStr = parts[1];

            // Fuente [64]: El servidor valida la solución
            if (currentPayload != null) {
                String toCheck = currentPayload + saltStr;
                String hash = HashUtils.getSha256(toCheck);

                // IMPORTANTE: Dificultad 4 ceros para que coincida con el cliente
                if (HashUtils.checkHash(hash, 4)) {
                    MiningGuiServer.notifySolutionFound("Cliente " + this.getId());
                } else {
                    MiningGuiServer.log("ADVERTENCIA: Cliente " + this.getId() + " envió solución INCORRECTA.");
                }
            }
        } catch (Exception e) {
            MiningGuiServer.log("Error procesando solución: " + e.getMessage());
        }
    }

    private void disconnect() {
        try {
            MiningGuiServer.clients.remove(this);
            socket.close();
            MiningGuiServer.log("Cliente desconectado.");
            MiningGuiServer.updateClientCount();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}