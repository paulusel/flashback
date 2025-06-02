package org.flashback.types;

public class IdResponse extends ServerResponse {
    private int id;
    public IdResponse(boolean ok, int statusCode, int data) {
        super(ok, statusCode);
        this.id= data;
    }

    public int getData() {
        return id;
    }

    public void SetData(int id) {
        this.id = id;
    }
}
