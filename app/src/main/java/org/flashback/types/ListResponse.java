package org.flashback.types;

import java.util.List;

public class ListResponse<T> extends ServerResponse { 
    private List<T> list;

    public ListResponse() {}

    public ListResponse(boolean ok, int statusCode, List<T> list) {
        super(ok, statusCode);
        this.list = list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public List<T> getList() {
        return list;
    }
}
