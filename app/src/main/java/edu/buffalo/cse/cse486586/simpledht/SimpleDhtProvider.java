package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.w3c.dom.Node;

public class SimpleDhtProvider extends ContentProvider {
    final String JOIN_RQST = "JOIN";
    final String OK_JOIN = "OK";
    final String CNTR_NODE = "11108";
    final String UPDATE_PRE = "PRE";
    final String UPDATE_SUC = "SUC";
    final String FORWARD = "FORWARD";
    final String SENDALL = "SENDALL";
    final String QUERY = "QUERY";
    final String RESPONSE = "RESPONSE";
    final String PHANTOM = "PHANTOM";

    final int MASTER_MODE  = 666;


    final String REMOTE_PORT0 = "11108";
    final String REMOTE_PORT1 = "11112";
    final String REMOTE_PORT2 = "11116";
    final String REMOTE_PORT3 = "11120";
    final String REMOTE_PORT4 = "11124";

    final int JOIN_NUM = 1;
    final int OK_JOIN_PRED = 2;
    final int OK_JOIN_SUCC = 3;
    final int UPDATE_NUM = 4;

    final int OWN_ZONE = 0;
    final int OTHER_ZONE = 1;

    final int FORWARD_FILE = 0;
    final int FORWARD_VAL = 1;

    final int SINGLE_ONLY = 1;
    final int ALL_ONLY = 2;

    final int KEY_MODE = 1;
    final int VAL_MODE = 2;

    final int QUERY_PORT = 1;
    final int QUERY_KEY = 2;
    static String stringMaster;

    static boolean GOT_IT = false;
    static int jack = 0;
    //Using Locks
    //References- http://examples.javacodegeeks.com/core-java/util/concurrent/locks-concurrent/condition/java-util-concurrent-locks-condition-example/
    //End
    //private final Lock lock = new ReentrantLock();
    //private final Condition gotIt = lock.newCondition();

    static List<NodeHashList> listOfHash = new ArrayList<NodeHashList>();

    final int SERVER_PORT = 10000;
    static NodeInfo myNode = new NodeInfo();
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
        Log.v("INSERT: ","BEGIN");
        String keyFile = values.getAsString("key");
        String contentFile = values.getAsString("value");

        Log.v("INSERT: what",keyFile+"!"+contentFile);
        //Perform the checks and validations

        int decision  = 99;
        try {



            decision = insertWorthy(keyFile);
            switch (decision){
                case OWN_ZONE:   createContent(keyFile,contentFile);
                    break;
                case OTHER_ZONE: forwardToSucc(keyFile,contentFile);
                    break;
                default: Log.e("Insert","Unable to fine zone");
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Insert the entries into the file named "KEYFILE"

        Log.v("INSERT: ","END");
        return null;
    }

    private void forwardToSucc(String keyFile, String contentFile) {
        Log.v("ForwardToSucc",keyFile+"!"+contentFile);
        String msg = FORWARD+"~"+keyFile+"!"+contentFile;
        sendToOtherSide(myNode.succPort,msg);
    }

    private void createContent(String keyFile, String contentFile) throws IOException {
        FileOutputStream fpOutput = getContext().openFileOutput(keyFile, Context.MODE_PRIVATE);
        byte[] bitVal = contentFile.getBytes();
        fpOutput.write(bitVal);
        fpOutput.close();
        fpOutput.flush();
    }

    private int insertWorthy(String keyFile) throws NoSuchAlgorithmException {
        String hashKeyFile = genHash(keyFile);

        if(myNode.predCode == null && myNode.succCode == null){
            Log.i("SIGLENODE",keyFile+"-+-"+hashKeyFile);
            return OWN_ZONE;
        }
        String predCode = genHash(getRightID(myNode.predPort));
        String succCode = genHash(getRightID(myNode.succPort));

        if(predCode.compareTo(myNode.myid)>0){

            //I am in corner case
            Log.v("InsertWorthy: corner",hashKeyFile+"-+-"+myNode.myid);
            if(hashKeyFile.compareTo(predCode)>0 || hashKeyFile.compareTo(myNode.myid)<=0){
                Log.v("InsertWorthy: c1",hashKeyFile+"-+-"+myNode.myid);
                return OWN_ZONE;        //Node is greater than pred and smaller than myID
            }
//            if(hashKeyFile.compareTo(myNode.predCode)<0 && hashKeyFile.compareTo(myNode.myid)<=0){
//                Log.v("InsertWorthy: c2",hashKeyFile+"-+-"+myNode.myid);
//                return OWN_ZONE;        //Node is smaller than pred and smaller than myID
//            }
            else{

                return OTHER_ZONE;
            }
        }

        else{

            if(hashKeyFile.compareTo(predCode)>0 && hashKeyFile.compareTo(myNode.myid)<=0){
                return OWN_ZONE;
            }else {
                return OTHER_ZONE;
            }

        }
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
        String[] colVal = new String[]{"key","value"};
        MatrixCursor tbl = new MatrixCursor(colVal);
        int decision;
        Log.i("SELECTION",selection);
        try {
            if(selection.equals("*")){
                queryAllEval();
            }else if(selection.equals("@")){
                tbl = getAll(tbl);
            }else{
                decision = insertWorthy(selection);
                switch (decision){
                    case OTHER_ZONE:        return searchSingle(QUERY+"|"+myNode.myport+"*"+selection);
                                             //Wait till I get

                    case OWN_ZONE:  return queryDo(selection, tbl);

                    default:    Log.v("Query","Cannot decide");
                }
            }




            tbl = queryDo(selection,tbl);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String msg = null;
        return tbl;
    }
    //--Srini
    private Cursor searchSingle(String msg) throws InterruptedException {
        GOT_IT = false;
        //sendToOtherSide(myNode.succPort,msg);

        while(true){
            if(GOT_IT){
                break;
            }
            Thread.sleep(10,0);
        }
        GOT_IT = false;

        String key = extractKeyVal(msg,KEY_MODE);
        String val = extractKeyVal(msg,VAL_MODE);
        String[] colVal = new String[]{"key","value"};
        MatrixCursor tbl = new MatrixCursor(colVal);
        String[] str = new String[2];
        str[0] = key;
        str[1] = val;
        tbl.addRow(str);
    }
    //--Srini
    //--Srinivasan
    private String extractKeyVal(String msg, int val_mode) {
        int start = 0, end = 0;
        if(val_mode == KEY_MODE){
            for(int i =0; i<msg.length();i++){
                if(msg.charAt(i)=='<'){
                    start = i+1;
                }
                if(msg.charAt(i)=='|'){
                    end = i;
                }

            }
            return msg.substring(start,end);
        }
        if(val_mode == VAL_MODE){
            for(int i =0; i<msg.length();i++){
                if(msg.charAt(i)=='|'){
                    start = i+1;
                }
                if(msg.charAt(i)=='>'){
                    end = i;
                }

            }
            return msg.substring(start,end);
        }
        return  null;
    }
    //--Srini

    //--Srinivasan
    private void queryAllEval() throws IOException, InterruptedException {
        GOT_IT = false;
        stringMaster = keyValPair();
        sendToOtherSide(myNode.succPort,PHANTOM+"%"+myNode.myport);
        while(true){
            if(GOT_IT){
                break;
            }
            Thread.sleep(10,0);
        }
        GOT_IT=false;
    }
    //--Srinivasan


    //--Srinivasan
    private String keyValPair() throws IOException {
        String[] lFiles = new String[100];
        String kvpair =null;
        lFiles = getContext().fileList();

        FileInputStream fpInput = null;
        String[] valueSet = new String[2];
        for (String fName : lFiles) {
            Log.i("FILES", fName);
            fpInput = getContext().openFileInput(fName);
            BufferedReader br = new BufferedReader(new InputStreamReader(fpInput));
            valueSet[1] = br.readLine();
            valueSet[0] = fName;
            kvpair = "<" + valueSet[0] + "|" + valueSet[1] + ">";
        }
        return kvpair;
    }
    //--Srinivasan


    private MatrixCursor storeIntoTable(String stringMaster,MatrixCursor tbl) {

        //String format <key,val>
        return tbl;
    }

    private Cursor addTbl(String resultAll, MatrixCursor tbl) {
        String key = getKeyValue (resultAll,KEY_MODE);
        String val = getKeyValue (resultAll,VAL_MODE);

        String[] str = new String[2];
        str[0] = key;
        str[1] = val;

        tbl.addRow(str);
        return tbl;
    }

    private String getKeyValue(String result, int MODE){
        if(MODE == KEY_MODE){
            for(int i =0; i<result.length();i++){
                if(result.charAt(i)==':'){
                    return result.substring(0,i);
                }
            }
        }
        if(MODE == VAL_MODE){
            for(int i =0; i<result.length();i++){
                if(result.charAt(i)==':'){
                    return result.substring(i+1);
                }
            }
        }
        return null;
    }

    private MatrixCursor getAll(MatrixCursor tbl) throws IOException {
        String[] lFiles = new String[100];

        lFiles=getContext().fileList();

        FileInputStream fpInput = null;
        String[] valueSet = new String[2];
        for(String fName: lFiles){
            Log.i("FILES",fName);
            fpInput = getContext().openFileInput(fName);
            BufferedReader br = new BufferedReader(new InputStreamReader(fpInput));
            valueSet[1] = br.readLine();
            valueSet[0] = fName;
            tbl.addRow(valueSet);
            Log.i("ALL",":"+fName +" : "+ valueSet[1]);
        }

        return tbl;
    }

/*

    private String seekQuery(String selection, int MODE) throws InterruptedException {
        String output,msg=null;
        stringMaster = null;

            msg = QUERY+"+"+myNode.myport+"$"+selection;

        sendToOtherSide(myNode.succPort,msg);
        while(true){

            if(GOT_IT)
                break;
        }
        GOT_IT = false;
        return stringMaster;
    }
*/



    //--Srinivasan
    private MatrixCursor queryDo(String selection, MatrixCursor tbl) throws IOException {

        FileInputStream fpInput = null;
        fpInput = getContext().openFileInput(selection);
        BufferedReader br = new BufferedReader(new InputStreamReader(fpInput));
        String value = br.readLine();
        String[] str = new String[2];
        str[0]=selection;
        str[1] = value;
        tbl.addRow(new String[]{selection,value});
        return tbl;
    }
    //--Srinivasan
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
                        Log.v("UDPATE SUCC: ",msg);
                        serveUpdatePre(msg);
                    }
                    if(msg.toLowerCase().contains(UPDATE_SUC.toLowerCase())){
                        Log.v("UDPATE PRE: ",msg);
                        serveUpdateSuc(msg);
                    }
                    if(msg.toLowerCase().contains(FORWARD.toLowerCase())){
                        serveForward(msg);
                    }
                    if(msg.toLowerCase().contains(QUERY.toLowerCase())){
                        serveQuery(msg);
                    }
                    if(msg.toLowerCase().contains(RESPONSE.toLowerCase())){
                        serveResponse(msg);

                    }
                    if(msg.toLowerCase().contains(PHANTOM.toLowerCase())){
                        servePhantom(msg);
                    }
                    Log.i("CONSENSUS","P-"+myNode.predPort+"("+myNode.myport+")"+"S-"+myNode.succPort);
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

    private void servePhantom(String msg) {

    }


    private void serveResponse(String msg) {
        for(int i = 0; i < msg.length();i++){
            if(msg.charAt(i)=='$'){
                stringMaster = msg.substring(i+1);
            }
        }
        GOT_IT = true;
    }

    private String resolveKeyVal(String msg, int MODE) {
        if(MODE == KEY_MODE){
            for(int i =0; i<msg.length();i++){
                if(msg.charAt(i)==':'){
                    return msg.substring(0,i);
                }
            }
        }
        if(MODE == VAL_MODE){
            for(int i =0; i<msg.length();i++){
                if(msg.charAt(i)==':'){
                    return msg.substring(i+1);
                }
            }
        }
        return null;
    }
    //--Srinivasan
    private void serveQuery(String msg) throws NoSuchAlgorithmException, IOException {

        String senderPort = extractKeyPort(msg,QUERY_PORT);
        String keyVal = extractKeyPort(msg,QUERY_KEY);
        String hashKey = genHash(keyVal);

        int desc = insertWorthy(hashKey);

        switch(desc){
            case OWN_ZONE:  sendToOtherSide(senderPort,RESPONSE+"$"+queryThis(keyVal));
                            break;
            case OTHER_ZONE:sendToOtherSide(myNode.succPort,msg);
                            break;
            default: Log.e("serveQuery","NONE found suitable");
        }

    }
    //--Srinivasan
    private String queryThis(String keyVal) throws IOException {
        String strv;
        FileInputStream fpInput = null;
        fpInput = getContext().openFileInput(keyVal);
        BufferedReader br = new BufferedReader(new InputStreamReader(fpInput));
        String value = br.readLine();
        String[] str = new String[2];
        str[0]=keyVal;
        str[1] = value;
        strv = "<"+str[0]+"|"+str[1]+">";
        return strv;
    }
    //--Srinivasan
    //--Srinivasan

    //--Srinivasan
    private String extractKeyPort(String msg,int MODE) {
        int start = 0, end = 0;
        if (MODE == QUERY_PORT) {
            for (int i = 0; i < msg.length(); i++) {
                if (msg.charAt(i) == '|') {
                    start = i + 1;
                }
                if (msg.charAt(i) == '*') {
                    end = i;
                    break;
                }
            }
            return msg.substring(start, end);
        }
        if (MODE == QUERY_KEY) {
            for (int i = 0; i < msg.length(); i++) {
                if (msg.charAt(i) == '*') {
                    return msg.substring(i + 1);
                }
            }


        }
        return null;
    }
    //--Srinivasan

    private void getInMe(String senderPort,String msg) throws IOException {
        String strVal=null;
        FileInputStream fpInput = null;
        if(msg.charAt(5)=='+'){ //Single entry
            String keyVal = extractKeyPort(msg,QUERY_KEY);
            fpInput = getContext().openFileInput(keyVal);
            BufferedReader br = new BufferedReader(new InputStreamReader(fpInput));
            String value = br.readLine();

            strVal = RESPONSE+"#"+keyVal+">"+value;
            sendToOtherSide(senderPort,strVal);
        }
    }

    private void serveForward(String msg) throws NoSuchAlgorithmException, IOException {
        String keyFile = extractFVName(msg, FORWARD_FILE);
        String valFile = extractFVName(msg,FORWARD_VAL);

        int desc = insertWorthy(keyFile);
        Log.v("ServeFor: pair",keyFile+"!"+valFile);

        switch (desc){
            case OWN_ZONE:  createContent(keyFile,valFile);
                break;
            case OTHER_ZONE: forwardToSucc(keyFile,valFile);
                break;
            default:    Log.e("serveForward","none identified");
        }
    }

    private void serveUpdateSuc(String msg) throws NoSuchAlgorithmException {
        String sucPort = extractPortNumber(msg, UPDATE_NUM);
        String hashSPort = genHash(getRightID(sucPort));

        myNode.predCode = hashSPort;
        myNode.predPort = sucPort;

        Log.v("DHT servUpd PRE ","PRED: "+myNode.predPort+" SUCC: "+myNode.succPort);
    }

    private void serveUpdatePre(String msg) throws NoSuchAlgorithmException {
        String prePort = extractPortNumber(msg, UPDATE_NUM);
        String hashPPort = genHash(getRightID(prePort));

        myNode.succCode = hashPPort;
        myNode.succPort = prePort;
        Log.v("DHT servUpd SUC", "PRED: " + myNode.predPort + " SUCC: " + myNode.succPort);
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
                            sendMessage = null;
                            sendMessage = OK_JOIN+"*"+n1.portNum+"^"+n2.portNum;
                            Log.v("DHT 1st MID",sendMessage);
                            sendToOtherSide(portOfJoin,sendMessage);

                            sendMessage = null;

                            sendMessage = UPDATE_PRE+"*"+nhl.portNum;
                            Log.v("DHT 2nd MID",sendMessage);
                            sendToOtherSide(n1.portNum,sendMessage);

                            sendMessage = null;

                            sendMessage = UPDATE_SUC+"^"+nhl.portNum;
                            Log.v("DHT 3rd MID",sendMessage);
                            sendToOtherSide(n2.portNum,nhl.portNum);

                            /*myNode.predPort = n1.portNum;
                            myNode.predCode = n1.hashCode;

                            myNode.succPort = n2.portNum;
                            myNode.succCode = n2.hashCode;*/
                        }else if(listOfHash.indexOf(i)==0){
                            NodeHashList n1 = listOfHash.get(listOfHash.size()-1);
                            NodeHashList n2 = listOfHash.get(listOfHash.indexOf(i)+1);
                            sendMessage = null;
                            sendMessage = OK_JOIN+"*"+n1.portNum+"^"+n2.portNum;
                            sendToOtherSide(portOfJoin,sendMessage);
                            Log.v("DHT 1st TOP",sendMessage);
                            sendMessage = null;

                            sendMessage = UPDATE_PRE+"*"+nhl.portNum;
                            sendToOtherSide(n1.portNum,sendMessage);
                            Log.v("DHT 2nd TOP ",sendMessage);
                            sendMessage = null;

                            sendMessage = UPDATE_SUC+"^"+nhl.portNum;
                            sendToOtherSide(n2.portNum,sendMessage);
                            Log.v("DHT 3rd TOP",sendMessage);

                            /*myNode.predPort = n1.portNum;
                            myNode.predCode = n1.hashCode;

                            myNode.succPort = n2.portNum;
                            myNode.succCode = n2.hashCode;*/
                        }else if(listOfHash.indexOf(i) == listOfHash.size()-1){
                            NodeHashList n1 = listOfHash.get(listOfHash.indexOf(i)-1);
                            NodeHashList n2 = listOfHash.get(0);
                            sendMessage = null;

                            sendMessage = OK_JOIN+"*"+n1.portNum+"^"+n2.portNum;
                            sendToOtherSide(portOfJoin,sendMessage);
                            Log.v("DHT 1st BOT",sendMessage+" to "+portOfJoin);
                            sendMessage = null;

                            sendMessage = UPDATE_PRE+"*"+nhl.portNum;
                            sendToOtherSide(n1.portNum,sendMessage);
                            Log.v("DHT 2nd BOT",sendMessage+" to "+n1.portNum);
                            sendMessage = null;

                            sendMessage = UPDATE_SUC+"^"+nhl.portNum;
                            sendToOtherSide(n2.portNum,sendMessage);
                            Log.v("DHT 3rd BOT",sendMessage+" to "+n2.portNum);

                           /* myNode.predPort = n1.portNum;
                            myNode.predCode = n1.hashCode;

                            myNode.succPort = n2.portNum;
                            myNode.succCode = n2.hashCode;*/
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

        Log.v("JOINED ","PRED: "+myNode.predPort+"%"+" SUCC: "+myNode.succPort);
    }

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
    private String extractFVName(String msg, int MODE) {
        int start = 0, end = 0;
        if (MODE == FORWARD_FILE) {
            // ~ for file name
            for (int i = 0; i < msg.length(); i++) {
                if (msg.charAt(i) == '~') {
                    start = i + 1;
                }
                if (msg.charAt(i) == '!') {
                    end = i;
                }
            }
            return msg.substring(start, end);
        }
        if (MODE == FORWARD_VAL) {
            // ! for value name
            for (int i = 0; i < msg.length(); i++) {
                if (msg.charAt(i) == '!') {
                    return msg.substring(i + 1);
                }
            }
        }
        return null;
    }

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
