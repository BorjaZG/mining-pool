package common;

// Representa una transacción: "mv|cantidad|origen|destino"
public record TransactionRecord(String origen, String destino, int cantidad) {
    @Override
    public String toString() {
        // Formato obligatorio según el PDF
        return "mv|" + cantidad + "|" + origen + "|" + destino;
    }
}