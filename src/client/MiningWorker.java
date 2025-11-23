package client;

import common.HashUtils;

public class MiningWorker extends Thread {
    private final long startRange;
    private final long endRange;
    private final String payload;
    private final MinerClient clientCallback; // Para avisar al jefe si encontramos oro

    public MiningWorker(long startRange, long endRange, String payload, MinerClient client) {
        this.startRange = startRange;
        this.endRange = endRange;
        this.payload = payload;
        this.clientCallback = client;
    }

    @Override
    public void run() {
        // Fuente [52]: "...clientes irán añadiendo un número... contenido + salt"
        for (long salt = startRange; salt <= endRange; salt++) {

            // Comprobar si el servidor nos mandó parar (interrupt)
            if (Thread.interrupted()) {
                System.out.println("Worker detenido por orden del servidor.");
                return;
            }

            // Calcular Hash
            String toHash = payload + salt;
            String hash = HashUtils.getSha256(toHash);

            // Verificar dificultad (usamos 4 ceros como en el servidor)
            // Fuente [52]: "empiece por 2 ceros" (nosotros usamos 4 para que cueste un poco más)
            if (HashUtils.checkHash(hash, 4)) {
                // ¡ÉXITO!
                clientCallback.sendSolution(salt);
                return; // Termina el hilo
            }
        }
        System.out.println("Rango terminado sin solución.");
    }
}