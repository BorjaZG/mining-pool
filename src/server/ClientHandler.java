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
                    int count = MiningGuiServer.clients.size();
                    sendMessage(Protocol.RESP_ACK + " " + count + " total clients");

                    MiningGuiServer.log("Cliente registrado (ID: " + this.getId() + ")");
                    MiningGuiServer.updateClientCount();

                }
                // --- NUEVO: RECEPCIÓN DE TRANSACCIÓN ---
                else if (inputLine.startsWith(Protocol.CMD_SEND_TX)) {
                    // Formato esperado: tx <datos>
                    String txData = inputLine.substring(Protocol.CMD_SEND_TX.length()).trim();
                    // Añadimos quién la envió
                    String fullTx = "Cliente" + this.getId() + "->" + txData;

                    // Mandamos al servidor central
                    MiningGuiServer.addTransaction(fullTx);
                }
                // ---------------------------------------

                else if (inputLine.startsWith(Protocol.CMD_SOL)) {
                    handleSolution(inputLine);
                }
            }
        } catch (IOException e) {
            // Cliente desconectado
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

            if (currentPayload != null) {
                String toCheck = currentPayload + saltStr;
                String hash = HashUtils.getSha256(toCheck);

                // Dificultad: 4 ceros
                if (HashUtils.checkHash(hash, 4)) {
                    MiningGuiServer.notifySolutionFound("Cliente " + this.getId());
                } else {
                    MiningGuiServer.log("Solución incorrecta recibida del Cliente " + this.getId());
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