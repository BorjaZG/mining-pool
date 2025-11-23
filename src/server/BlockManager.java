package server;

import common.Protocol;
import common.TransactionRecord;
import java.util.Random;

public class BlockManager extends Thread {

    @Override
    public void run() {
        while (true) {
            try {
                // Simular espera de 5 minutos (ponemos 20 segundos para probar)
                // Fuente [25]: "El mining pool generará un nuevo 'bloque' cada 5 minutos"
                Thread.sleep(20000);

                // Si no hay clientes, no generamos trabajo
                if (MiningServer.clients.isEmpty()) {
                    System.out.println("Esperando clientes para iniciar minado...");
                    continue;
                }

                System.out.println("\n--- GENERANDO NUEVO BLOQUE ---");
                MiningServer.solved = false; // Reiniciamos bandera

                // 1. Generar transacciones falsas
                // Fuente [60]: "Formato similar a un movimiento de una cuenta origen a una destino"
                String payload = generarTransacciones();
                System.out.println("Payload generado: " + payload);

                // 2. Repartir rangos entre los clientes conectados
                // Fuente [37]: "server > client1: new_request 0-100..."
                int totalClients = MiningServer.clients.size();
                int rangeSize = 1000000; // Un millón de números por cliente
                int start = 0;

                for (ClientHandler client : MiningServer.clients) {
                    int end = start + rangeSize;

                    // Guardamos el payload en el cliente para que pueda validarlo luego si gana
                    client.setCurrentPayload(payload);

                    // Construir mensaje: new_request INICIO-FIN PAYLOAD
                    String msg = Protocol.RESP_NEW_REQUEST + " " + start + "-" + end + " " + payload;
                    client.sendMessage(msg);

                    start = end + 1;
                }

                System.out.println("Trabajo distribuido a " + totalClients + " clientes.");

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
            String origen = "user" + rand.nextInt(100);
            String destino = "user" + rand.nextInt(100);
            int cantidad = rand.nextInt(1000);

            TransactionRecord tx = new TransactionRecord(origen, destino, cantidad);
            sb.append(tx.toString()).append(";");
        }
        return sb.toString();
    }
}