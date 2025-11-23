package common;

public class Protocol {
    // Comandos del Cliente
    public static final String CMD_CONNECT = "connect";
    public static final String CMD_SOL = "sol"; // Ejemplo: "sol 34521"

    // Comandos del Servidor
    public static final String RESP_ACK = "ack"; // Confirmación general
    public static final String RESP_NEW_REQUEST = "new_request"; // Envío de trabajo
    public static final String RESP_END = "end"; // Fin del trabajo actual

    // Configuración por defecto
    public static final int DEFAULT_PORT = 12345;
    public static final String DEFAULT_HOST = "localhost";
}