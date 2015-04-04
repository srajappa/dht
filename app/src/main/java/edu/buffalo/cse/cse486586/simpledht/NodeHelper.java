package edu.buffalo.cse.cse486586.simpledht;

import org.w3c.dom.Node;

/**
 * Created by srajappa on 4/1/15.
 */
public class NodeHelper {
    String prev;
    String next;
    String predPort;
    String succPort;
    String myport;
    String note;

    NodeHelper(){
        prev = null;
        next = null;
    }
    NodeHelper(String prev, String next,String predPort, String succPort, String myport, String note){
        this.prev = prev;
        this.next = next;
        this.predPort = predPort;
        this.succPort = succPort;
        this.note = note;
        this.myport = myport;
    }
}
