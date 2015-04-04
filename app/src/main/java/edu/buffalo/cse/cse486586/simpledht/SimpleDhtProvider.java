package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.w3c.dom.Node;

public class SimpleDhtProvider extends ContentProvider {
    final String JOIN_RQST = "JOIN";
    final String OK_JOIN = "OK";
    final String CNTR_NODE = "11108";
    final String UPDATE = "UPDATE";
    final String FORWARD = "FORWARD";
    final String UPDATE_PRE = "UPDATEPRE";
    final String UPDATE_SUC = "UPDATESUC";

    final int MASTER_MODE  = 666;


    final int REMOTE_PORT0 = 5554;
    final int REMOTE_PORT1 = 5556;
    final int REMOTE_PORT2 = 5558;
    final int REMOTE_PORT3 = 5560;
    final int REMOTE_PORT4 = 5562;

    final int JOIN_NUM = 1;
    final int OK_JOIN_PRED = 2;
    final int OK_JOIN_SUCC = 3;
    final int UPDATE_NUM = 4;

    final int OWN_ZONE = 0;
    final int OTHER_ZONE = 1;
    final int

    List<NodeHashList> listOfHash = new ArrayList<NodeHashList>();

    final int SERVER_PORT = 10000;
    static NodeInfo myNode = null;
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        String keyFile = values.getAsString("key");
        String contentFile = values.getAsString("value");

        //Perform the checks and validations

        int decision  = insertWorthy(keyFile);
        switch (decision){
            case OWN_ZONE:
        }

        //Insert the entries into the file named "KEYFILE"

        return null;
    }

    private int insertWorthy(String keyFile) throws NoSuchAlgorithmException {
        String hashKeyFile = genHash(keyFile);

        if()
    }

    /*
     * Creates a provision to find out the central node
     * i.e. 5554 and perform an async task to get the
     * messages passing.
     * @author: Srinivasan
     */
    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub

        TelephonyManager telpmngr = (TelephonyManager)getContext().getSystemService(getContext().TELEPHONY_SERVICE);
        String portStr = telpmngr.getLine1Number().substring(telpmngr.getLine1Number().length() - 4) ;
        final String myServerPort =  String.valueOf(Integer.parseInt(portStr)*2);

        //establish server irrespective of who or what the node is
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            Log.v("DHT: onCreate","Creating a server ("+myServerPort+")");
            //setting up node for self
            myNode = new NodeInfo(null,null,genHash(getRightID(myServerPort)),myServerPort,null,null);

        } catch (IOException e) {
            Log.e("DHT: onCreate", "Can't create a ServerSocket");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


        if(myServerPort.equals(CNTR_NODE)){         //Identified as a mother node and it will contain every detail
            //NodeInfo motherNode = new NodeInfo(genHash(myServerPort), genHash(myServerPort), genHash(myServerPort),myServerPort);
            myNode.masterMode(MASTER_MODE);
            try {
                listOfHash.add(new NodeHashList(genHash(getRightID(myServerPort)),myServerPort));          //Add this
                Log.v("DHT: SvrSocket(MASTER)",String.valueOf(Integer.parseInt(myServerPort) / 2));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            Log.v("DHT: ServerSocket","NO need to additional action (first master node)");
        }else{
            //Send JOIN_RQST to the node with port number 5554
            Log.v("DHT: JOIN SEND",myServerPort);
            sendToOtherSide(CNTR_NODE, JOIN_RQST+"|"+myServerPort);

            //receive the details of successor and predecessor
        }


        return false;

    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public void sendToOtherSide(String destnPort, String message){
        int port = Integer.parseInt(destnPort);
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, String.valueOf(port));
    }



    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            //--Srinivasan BEGIN
            String uqID,senderPort, hashID;
            while(true){
                try{
                    Socket s = serverSocket.accept();
                    PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
                    BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    String msg = br.readLine();
                    Log.v("RECV ",msg);
                    //if msg received is equivalent to JOIN
                    if(msg.toLowerCase().contains(JOIN_RQST.toLowerCase())){
                        //Lookup ID check and seek the perfect location to place
                        Log.v("DHT: JOIN RECV",msg);
                        serveJoin(msg);
                    }
                    if(msg.toLowerCase().contains(OK_JOIN.toLowerCase())){
                        //Should Update SUCC and PRED
                        serveOK_JOIN(msg);
                    }
                    if(msg.toLowerCase().contains(UPDATE_PRE.toLowerCase())){
                        serveUpdatePre(msg);
                    }
                    if(msg.toLowerCase().contains(UPDATE_SUC.toLowerCase())){
                        serveUpdateSuc(msg);
                    }

                }catch(SocketException s){
                    s.printStackTrace();
                }catch(IOException e){
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                //--Srinivasan END

            }


        }
    }

    private void serveUpdateSuc(String msg) throws NoSuchAlgorithmException {
        String sucPort = extractPortNumber(msg,UPDATE_NUM);
        String hashSPort = genHash(getRightID(sucPort));

        myNode.succCode = hashSPort;
        myNode.succPort = sucPort;

        Log.v("DHT servUpd",myNode.succPort);
    }

    private void serveUpdatePre(String msg) throws NoSuchAlgorithmException {
        String prePort = extractPortNumber(msg,UPDATE_NUM);
        String hashPPort = genHash(getRightID(prePort));

        myNode.predCode = hashPPort;
        myNode.predPort = prePort;
        Log.v("DHT servUpd",myNode.predPort);
    }

    private String getRightID (String sub){
        return String.valueOf(Integer.parseInt(sub)/2);
    }

    private void serveJoin(String msg) throws NoSuchAlgorithmException {
        if(listOfHash.isEmpty()){
            Log.e("DHT NO listOfHash",myNode.myport);

        }else{
            String portOfJoin = extractPortNumber(msg,JOIN_NUM);
            String hashPort = genHash(getRightID(portOfJoin));
            String sendMessage = null;
            Log.v("DHT serveJoin",portOfJoin);
            NodeHashList nhl = new NodeHashList();
            //check if the hashPort is less than or greater than

            if(listOfHash.size()==1){
                myNode.predPort = portOfJoin;
                myNode.succPort = portOfJoin;
                myNode.predCode = genHash(getRightID(portOfJoin));
                myNode.succCode = genHash(getRightID(portOfJoin));
                listOfHash.add(new NodeHashList(genHash(getRightID(portOfJoin)),portOfJoin));
                sendMessage = OK_JOIN+"*"+myNode.myport+"^"+myNode.myport;
                Log.v("DHT firstJOIN",portOfJoin);
                sendToOtherSide(portOfJoin,sendMessage);            //Sending the Predecessor PORT  & Sending the Successor Port

            }else{
                nhl.hashCode = hashPort;
                nhl.portNum = portOfJoin;
                listOfHash.add(new NodeHashList(hashPort,portOfJoin));

                Log.v("DHT secondJoin",portOfJoin);

                sortListOfHash();

                for(NodeHashList i: listOfHash){
                    if(i.hashCode.equals(nhl.hashCode)){
                        if(listOfHash.indexOf(i)!=0 && listOfHash.indexOf(i)!=listOfHash.size()-1){
                            //It is in the middle
                            NodeHashList n1 = listOfHash.get(listOfHash.indexOf(i)-1);
                            NodeHashList n2 = listOfHash.get(listOfHash.indexOf(i)+1);

                            sendMessage = OK_JOIN+"*"+n1.portNum+"^"+n2.portNum;
                            Log.v("DHT 1st MID",sendMessage);
                            sendToOtherSide(portOfJoin,sendMessage);

                            sendMessage = null;

                            sendMessage = UPDATE_PRE+"*"+nhl.portNum;
                            Log.v("DHT 2nd MID",sendMessage);
                            sendToOtherSide(n2.portNum,sendMessage);

                            sendMessage = null;

                            sendMessage = UPDATE_SUC+"^"+nhl.portNum;
                            Log.v("DHT 3rd MID",sendMessage);
                            sendToOtherSide(n1.portNum,nhl.portNum);

                        }else if(listOfHash.indexOf(i)==0){
                            NodeHashList n1 = listOfHash.get(listOfHash.size()-1);
                            NodeHashList n2 = listOfHash.get(listOfHash.indexOf(i)+1);

                            sendMessage = OK_JOIN+"*"+n1.portNum+"^"+n2.portNum;
                            sendToOtherSide(portOfJoin,sendMessage);
                            Log.v("DHT 1st TOP",sendMessage);
                            sendMessage = null;

                            sendMessage = UPDATE_PRE+"*"+nhl.portNum;
                            sendToOtherSide(n2.portNum,sendMessage);
                            Log.v("DHT 2nd TOP ",sendMessage);
                            sendMessage = null;

                            sendMessage = UPDATE_SUC+"^"+nhl.portNum;
                            sendToOtherSide(n1.portNum,nhl.portNum);
                            Log.v("DHT 3rd TOP",sendMessage);
                        }else if(listOfHash.indexOf(i) == listOfHash.size()-1){
                            NodeHashList n1 = listOfHash.get(listOfHash.indexOf(i)-1);
                            NodeHashList n2 = listOfHash.get(0);


                            sendMessage = OK_JOIN+"*"+n1.portNum+"^"+n2.portNum;
                            sendToOtherSide(portOfJoin,sendMessage);
                            Log.v("DHT 1st BOT",sendMessage);
                            sendMessage = null;

                            sendMessage = UPDATE_PRE+"*"+nhl.portNum;
                            sendToOtherSide(n2.portNum,sendMessage);
                            Log.v("DHT 2nd BOT",sendMessage);
                            sendMessage = null;

                            sendMessage = UPDATE_SUC+"^"+nhl.portNum;
                            sendToOtherSide(n1.portNum,nhl.portNum);
                            Log.v("DHT 3rd BOT",sendMessage);
                        }
                        break;
                    }
                }

            }
        }
    }

    private void sortListOfHash() {

        Collections.sort(listOfHash, new MyComparator());

        for(NodeHashList i:listOfHash){
            Log.v("DHT Sort",i.hashCode+"~~"+i.portNum);

        }
    }

    /*
    * Method: serveForwardConc
    * Arguments: String msg
    * Return type: void
    * The FORWARD Conclusion message from the predecessor
    * will take final actions to seal the node within
    * ---------------------------------
    * @author: srajappa
    * Date: 2 April, 2015
    *//*
    private void serveForwardConc(String msg) {

    }*/

    /*
    * Method: serveForward
    * Arguments: String msg
    * Return type: void
    * The FORWARD message from the predecessor
    * will perform inspections and take appropriate actions
    * ---------------------------------
    * @author: srajappa
    * Date: 2 April, 2015
    *//*
    private void serveForward(String msg) throws NoSuchAlgorithmException {
        serveJoin(msg);
    }*/

    /*
    * Method: serveUpdate
    * Arguments: String msg
    * Return type: void
    * The UPDATE message from MASTER NODE
    * will ask the predecessor to be changed.
    * ---------------------------------
    * @author: srajappa
    * Date: 2 April, 2015
    *//*
    private void serveUpdate(String msg) throws NoSuchAlgorithmException {
        String updatePort = extractPortNumber(msg,UPDATE_NUM);  //The predecessor is here

        String hashUpdatePort = genHash(updatePort);

        myNode.predCode = hashUpdatePort;
        myNode.predPort = updatePort;
    }*/

    /*
    * Method: serveOK_JOIN
    * Arguments: String msg
    * Return type: void
    * The OK_JOIN from Master node will be used
    * in order to update it's information.
    * ---------------------------------
    * @author: srajappa
    * Date: 2 April, 2015
    */
    private void serveOK_JOIN(String msg) throws NoSuchAlgorithmException {
        String predPort = extractPortNumber(msg,OK_JOIN_PRED);
        String succPort = extractPortNumber(msg,OK_JOIN_SUCC);

        myNode.succPort = succPort;
        myNode.predPort = predPort;

        String hashPredPort = genHash(getRightID(predPort));
        String hashSuccPort = genHash(getRightID(succPort));

        myNode.succCode = hashSuccPort;
        myNode.predCode = hashPredPort;

        Log.v("DHT servOK",myNode.predPort+"%"+myNode.succPort);
    }

    /*
     * Method: serveJoin
     * Arguments: String msg
     * Return type: void
     * Incoming Join requests will be served by
     * probing the HashCode of the sender and
     * providing that with additional details of
     * successor and predecessor.
     * ---------------------------------
     * @author: srajappa
     * Date: 2 April, 2015
     */
    /*
    private void serveJoin(String msg) throws NoSuchAlgorithmException {
        String sendMessage= null;
        String codeOfPort = null;
        String portOfJoin = extractPortNumber(msg,JOIN_NUM);


        //Detect if the Node is the first in the CHAIN :
        //1. IF YES change SUCC and PRED values, send OK_JOIN messages to the new node
        //2. IF NO perform
        //  a. Inspection of the Sender and
        //  b. Send appropriate OK_JOIN / CHANGE + UPDATE messages

        //----1.  -----
        if(myNode.predCode == null && myNode.succCode == null) {

            codeOfPort = genHash(msg);
            myNode.succCode = myNode.predCode = codeOfPort;
            myNode.succPort = myNode.predPort = portOfJoin;

            sendMessage = OK_JOIN+"*"+myNode.myport+"^"+myNode.myport;
            sendToOtherSide(portOfJoin,sendMessage);            //Sending the Predecessor PORT  & Sending the Successor Port

            sendMessage = null;

        }
        //----2. PART A -----
        //      Three Phases of Inspection
        //      i. The HashCode of Joinee is greater than current HashCode and Less than Successor
        //      ii. The HashCode of Joinee is greater than Successor

        //----2. PART A SUB-PART i.-----
        int joinNcurr = codeOfPort.compareTo(myNode.myid);
        int joinNsucc = codeOfPort.compareTo(myNode.succCode);
        if(joinNcurr>0){
            if(joinNsucc <= 0){
                //Free to JOIN
                sendMessage = OK_JOIN+"*"+myNode.myport+"^"+myNode.succPort;;
                sendToOtherSide(portOfJoin,sendMessage);

                sendMessage = null;

                //Send the UPDATE statement the concerned
                //------2. PART B------

                sendMessage = UPDATE+"*"+portOfJoin;
                sendToOtherSide(myNode.succPort,sendMessage);

                //change the successor port values
                myNode.succPort = portOfJoin;

            }else if(joinNsucc > 0){
                //Forward a REQUEST message to the successive nodes for the node cannot be fit here
                //-------2. PART A SUB-PART ii.------
                sendMessage = FORWARD+"|"+portOfJoin+"#"+myNode.myport;
                sendToOtherSide(myNode.succPort,sendMessage);
            }
        }

    }
*/
    /*
     * Method: extractPortNumber
     * Arguments: String msg, int MODE
     * Return type: String
     * This function will return the port number
     * by going through the message.
     * ---------------------------------
     * @author: srajappa
     * Date: 2 April, 2015
     */

    private String extractPortNumber(String msg, int MODE) {
        int i,start=0,end=0;
        if(MODE == JOIN_NUM){
            for(i =0; i<msg.length() ; i++){
                if(msg.charAt(i)=='|'){
                    Log.v("DHT retPORT |",msg.substring(i+1));
                    return msg.substring(i+1);        //Returns the PORT for JOIN_RQST messages
                }
            }
        }
        if(MODE == OK_JOIN_PRED){
            for(i =0; i<msg.length() ; i++){
                if(msg.charAt(i)=='*'){
                       start = i;     //Returns the PORT for OK_JOIN (pred) messages
                }
                if(msg.charAt(i)=='^'){
                       end = i;
                }

            }
            Log.v("DHT retPORT *",msg.substring(start+1,end));
            return msg.substring(start+1,end);
        }
        if(MODE == OK_JOIN_SUCC){
            for(i =0; i<msg.length() ; i++){
                if(msg.charAt(i)=='^'){
                    Log.v("DHT retPORT ^",msg.substring(i+1));
                    return msg.substring(i+1);        //Returns the PORT for OK_JOIN (succ) messages
                }
            }
        }
        if(MODE == UPDATE_NUM){
            for(i =0; i<msg.length() ; i++){
                if(msg.charAt(i)=='*'){
                    Log.v("DHT U retPORT *",msg.substring(i));
                    return msg.substring(i+1);

                }
                if(msg.charAt(i)=='^'){
                    Log.v("DHT U retPORT ^",msg.substring(i));
                    return msg.substring(i+1);

                }
            }
        }
        return null;
    }

    /*
     * calculates the best values of successor and predecessor.
     *
     *//*
    private void performCalculation(String senderPort,String second,int mode_msg) throws NoSuchAlgorithmException {
        if(mode_msg == 0 ) {
            String uqHashPort = genHash(senderPort);
            String sHashPort = genHash(myNode.succ);
            String pHashPort = genHash(myNode.pred);


            if(myNode.succ == null && myNode.pred == null) {     //Checks the node if it is vacant
                myNode.succ = uqHashPort;
                myNode.pred = uqHashPort;
                String msg = "OKJOIN|" + "*" + myNode.myport + "^" + myNode.myport;//sending information to the other that request is granted
                Log.v("DHT performCalculation", "Sending pred and succ same " + myNode.myport);
                sendToOtherSide(senderPort, msg);
            }
            if(myNode.succ.compareTo(uqHashPort)<0){           //Node info will be sent to successor
                String msg = "CHECK|"+myNode.myport+"*"+uqHashPort;
                sendToOtherSide(senderPort,msg);
            }
            if(myNode.myport.compareTo(uqHashPort)<0){
                if(myNode.succ.compareTo(uqHashPort)>0){
                    //This is the intermediary node
                    String msg = "OKJOIN|"+"*"+myNode.myid+"^"+myNode.succ;
                    String msg2 = "UPDATE|^"+myNode.succ;
                    myNode.succ = uqHashPort;
                    sendToOtherSide(senderPort,msg);
                    sendToOtherSide(myNode.succPort,msg2);
                }
            }

        }else if (mode_msg ==1){                //This is a good way to get things done for OK

            myNode.pred = genHash(senderPort);
            myNode.succ = genHash(second);
        }
    }*/
/*
    private void performLookup(String uqHash) {
        //if()
    }

    public String uqIDFinder(String msg,int mode){
        int start=0, end=0;
        if(mode == 0){
            for(int i = 0 ; i < msg.length(); i++){

                    if (msg.charAt(i) == '|') {
                        return msg.substring(i);
                    }
                }
        }else if(mode==1){
                for(int i = 0 ; i < msg.length(); i++){
                    if(msg.charAt(i)=='*'){
                        start = i;
                    }
                    if(msg.charAt(i)=='^'){
                        end = i;
                    }
                }
                return msg.substring(start,end-1);
        }else if(mode == 2){
            for (int i = 0; i<msg.length(); i++){
                if(msg.charAt(i)=='^'){
                    return msg.substring(i);
                }
            }
        }
        return  null;
    }*/


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... packet) {
            try {
                String remotePort = packet[1];
                Log.i("DHT: CLIENT",packet[1]);
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));

                String msgToSend = packet[0];
                Log.i("DHT: clientsends",msgToSend);
                OutputStream os = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os);

                BufferedWriter bos = new BufferedWriter(osw);
                bos.write(msgToSend);
                bos.flush();

                socket.close();
                Log.v("DHT: CLIENT SENT",msgToSend);
            } catch (UnknownHostException e) {
                Log.e("DHT", "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e("DHT", "ClientTask socket IOException");
            }

            return null;
        }
    }

}
