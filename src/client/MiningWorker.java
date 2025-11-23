package client;

import common.HashUtils;

public class MiningWorker extends Thread {
    private final long startRange;
    private final long endRange;
    private final String payload;
    private final MinerGuiClient clientCallback; // Cambiamos a la clase GUI

    public MiningWorker(long startRange, long endRange, String payload, MinerGuiClient client) {
        this.startRange = startRange;
        this.endRange = endRange;
        this.payload = payload;
        this.clientCallback = client;
    }

    @Override
    public void run() {
        long totalIterations = endRange - startRange;
        long counter = 0;

        for (long salt = startRange; salt <= endRange; salt++) {

            // 1. Comprobar si nos han detenido
            if (Thread.interrupted()) {
                return;
            }

            // 2. Actualizar la barra de progreso (Solo cada 1000 intentos para no congelar la UI)
            counter++;
            if (counter % 1000 == 0) {
                int percent = (int) ((counter * 100) / totalIterations);
                clientCallback.updateProgress(percent);
            }

            // 3. Calcular Hash (Igual que antes)
            String toHash = payload + salt;
            String hash = HashUtils.getSha256(toHash);

            // Verificar dificultad (usamos 4 ceros para pruebas)
            if (HashUtils.checkHash(hash, 4)) {
                clientCallback.updateProgress(100); // Completar barra
                clientCallback.sendSolution(salt);
                return;
            }
        }
    }
}