package net.coosanta.llm;

import java.io.Serializable;
import java.util.ArrayList;

public class Chat implements Serializable {
    public int id;
    private ArrayList<Response> content = new ArrayList<Response>();

    public Chat(Response response) {
        content.add(response);
    }
}

