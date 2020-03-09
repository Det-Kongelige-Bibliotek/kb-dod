package dk.kb.dod;

public class AlmaConnectionException extends Exception{
    public AlmaConnectionException(String message) {
        super(message);
    }

    public AlmaConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
