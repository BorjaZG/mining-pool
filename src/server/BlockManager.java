package server;

import common.Protocol;
import java.util.List;

public class BlockManager extends Thread {

    @Override
    public void run() {
        while (true) {
            try {
                // Comprobamos cada 2 segundos
                Thread.sleep(2000);

                // CONDICIÓN DE ACTIVACIÓN: Tener 3 o más transacciones pendientes
                if (MiningGuiServer.pendingTransactions.size() < 3) {
                    continue; // Seguimos esperando
                }

                if (MiningGuiServer.clients.isEmpty()) {
                    MiningGuiServer.log("Transacciones listas, pero faltan mineros...");
                    continue;
                }

                MiningGuiServer.log("\n--- ¡PACK DE 3 TRANSACCIONES COMPLETO! ---");
                MiningGuiServer.solved = false;

                // 1. Construir el bloque con las 3 primeras transacciones
                StringBuilder payloadBuilder = new StringBuilder();

                // Sacamos 3 de la lista (FIFO)
                for (int i = 0; i < 3; i++) {
                    String tx = MiningGuiServer.pendingTransactions.remove(0);
                    // Formato: mv|datos;
                    payloadBuilder.append("mv|").append(tx).append(";");
                }

                String payload = payloadBuilder.toString();
                MiningGuiServer.log("Generando bloque: " + payload);

                // 2. Repartir trabajo
                int totalClients = MiningGuiServer.clients.size();
                int rangeSize = 1000000;
                int start = 0;

                for (ClientHandler client : MiningGuiServer.clients) {
                    int end = start + rangeSize;
                    client.setCurrentPayload(payload);

                    String msg = Protocol.RESP_NEW_REQUEST + " " + start + "-" + end + " " + payload;
                    client.sendMessage(msg);
                    start = end + 1;
                }

                MiningGuiServer.log("¡Trabajo enviado a los mineros!");

                // Limpiar texto de estado
                MiningGuiServer.updateClientCount();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}