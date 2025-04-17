package org.flashback.database;

/**
 * DatabaseException
 */
public class DatabaseException extends Exception{

    public DatabaseException() {}

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(Throwable e) {
        super(e);
    }

    public DatabaseException(String message, Throwable e){
        super(message, e);
    }

}
