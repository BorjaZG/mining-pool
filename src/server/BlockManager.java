package server;

import common.Protocol;
import common.TransactionRecord;
import java.util.Random;

public class BlockManager extends Thread {

    @Override
    public void run() {
        while (true) {
            try {
                // Simulación del tiempo de bloque (20 segundos para pruebas)
                // Fuente [25]: Generar nuevo bloque periódicamente
                Thread.sleep(20000);

                if (MiningGuiServer.clients.isEmpty()) {
                    MiningGuiServer.log("Esperando clientes para iniciar minado...");
                    continue;
                }

                MiningGuiServer.log("\n--- GENERANDO NUEVO BLOQUE ---");
                MiningGuiServer.solved = false;

                // 1. Generar transacciones falsas
                // Fuente [60]: Formato similar a movimiento de cuenta
                String payload = generarTransacciones();
                MiningGuiServer.log("Datos del bloque: " + payload);

                // 2. Repartir rangos entre los clientes
                // Fuente [37]: Enviar new_request con rangos
                int totalClients = MiningGuiServer.clients.size();
                int rangeSize = 1000000; // 1 millón por cliente
                int start = 0;

                for (ClientHandler client : MiningGuiServer.clients) {
                    int end = start + rangeSize;

                    client.setCurrentPayload(payload);

                    String msg = Protocol.RESP_NEW_REQUEST + " " + start + "-" + end + " " + payload;
                    client.sendMessage(msg);

                    start = end + 1;
                }

                MiningGuiServer.log("Trabajo distribuido a " + totalClients + " clientes.");

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String generarTransacciones() {
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        // Generar 3 transacciones aleatorias
        for (int i = 0; i < 3; i++) {
            String origen = "u" + rand.nextInt(50);
            String destino = "u" + rand.nextInt(50);
            int cantidad = rand.nextInt(500);

            TransactionRecord tx = new TransactionRecord(origen, destino, cantidad);
            sb.append(tx.toString()).append(";");
        }
        return sb.toString();
    }
}