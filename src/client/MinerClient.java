package client;

import common.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MinerClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private MiningWorker currentWorker; // Referencia al hilo que está trabajando actualmente

    public static void main(String[] args) {
        new MinerClient().start();
    }

    public void start() {
        String host = Protocol.DEFAULT_HOST;
        int port = Protocol.DEFAULT_PORT;

        try {
            System.out.println("Conectando a " + host + ":" + port + "...");
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 1. Mandar mensaje de conexión inicial
            // Fuente [32, 45]: "client1: connect"
            out.println(Protocol.CMD_CONNECT);

            // 2. Bucle de escucha de mensajes del servidor
            String serverMsg;
            while ((serverMsg = in.readLine()) != null) {
                System.out.println("SERVIDOR DICE: " + serverMsg);
                processMessage(serverMsg);
            }

        } catch (IOException e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }

    private void processMessage(String msg) {
        // Mensaje: new_request INICIO-FIN PAYLOAD
        // Fuente [37, 50]: "new_request: origin-destino trama"
        if (msg.startsWith(Protocol.RESP_NEW_REQUEST)) {
            // Detener trabajo anterior si lo hubiera
            stopWorker();

            // Parsear el mensaje
            // Ejemplo: "new_request 0-100000 mv|10|a|b;..."
            try {
                String[] parts = msg.split(" ");
                String rangePart = parts[1]; // "0-100000"
                String payload = parts[2];   // "mv|..."

                String[] range = rangePart.split("-");
                long start = Long.parseLong(range[0]);
                long end = Long.parseLong(range[1]);

                System.out.println(">>> ¡A MINAR! Rango: " + start + " a " + end);

                // Confirmar recepción
                // Fuente [38]: "client1: ack"
                out.println(Protocol.RESP_ACK);

                // Iniciar el hilo trabajador
                // Fuente [63]: "Los clientes serán capaces de aceptar las peticiones... y ejecutar la búsqueda"
                currentWorker = new MiningWorker(start, end, payload, this);
                currentWorker.start();

            } catch (Exception e) {
                System.err.println("Error parseando new_request: " + e.getMessage());
            }

        }
        // Mensaje: end
        // Fuente [51]: "end: un cliente ha encontrado la solución, no es necesario seguir buscando"
        else if (msg.startsWith(Protocol.RESP_END)) {
            System.out.println(">>> STOP. Alguien más lo encontró.");
            stopWorker();
        }
    }

    private void stopWorker() {
        if (currentWorker != null && currentWorker.isAlive()) {
            currentWorker.interrupt(); // Interrumpir el hilo suavemente
        }
    }

    // Método que llamará el Worker si encuentra la solución
    public synchronized void sendSolution(long salt) {
        // Fuente [41, 63]: "client1: sol 36"
        System.out.println("!!! ENCONTRADO !!! Enviando solución: " + salt);
        out.println(Protocol.CMD_SOL + " " + salt);
    }
}