package dk.kb.dod;

public class AlmaConnectionException extends Exception{

    private static final long serialVersionUID = 1L;

    public AlmaConnectionException(String message) {
        super(message);
    }

    public AlmaConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlmaConnectionException() {
        super();
    }

    public AlmaConnectionException(Throwable cause) {
        super(cause);
    }

}
