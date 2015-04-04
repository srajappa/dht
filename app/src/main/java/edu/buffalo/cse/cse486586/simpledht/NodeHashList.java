package edu.buffalo.cse.cse486586.simpledht;

/**
 * Created by srajappa on 4/3/15.
 */
public class NodeHashList {
    public String hashCode;
    public String portNum;

    NodeHashList(String hashCode,String portNum){
        this.hashCode = hashCode;
        this.portNum = portNum;
    }

    public NodeHashList() {
        this.hashCode =null;
        this.portNum = null;
    }
}
