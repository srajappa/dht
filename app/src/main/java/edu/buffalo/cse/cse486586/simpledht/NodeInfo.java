package edu.buffalo.cse.cse486586.simpledht;

/**
 * Contains details of successor
 * predecessor and node ID
 * Created by srajappa on 3/31/15.
 */
public class NodeInfo {
     String predCode;
     String succCode;
     String myid;
     String myport;
     String predPort;
     String succPort;

     private int nodeMASTER;


    NodeInfo(){

    }
    NodeInfo(String pred, String succ, String myid,String myport, String predPort,String succPort){
        this.predCode = pred;
        this.succCode = succ;
        this.myid = myid;
        this.myport = myport;
        this.predPort = predPort;
        this.succPort = succPort;
    }

    void masterMode(int nodeMASTER){
        this.nodeMASTER = nodeMASTER;
    }

    boolean inMaster(){
        if(this.nodeMASTER == 666){
            return true;
        }else
            return false;
    }

    void updateDetails(String pred, String succ){
        this.predCode = pred;
        this.succCode = succ;

    }


        //return null;
}
