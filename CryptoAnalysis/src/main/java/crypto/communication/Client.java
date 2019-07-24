package crypto.communication;

import java.util.Arrays;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.lang.String;
import java.net.ServerSocket;
import crypto.HeadlessCryptoScanner;


public class Client {
    private Socket clientSocket;
    private PrintWriter out;
    private int port = 38401;
    //TODO replace, maybe not wanting to always do local tcp , but for now...
    private String ip = "127.0.0.1";
    
    public void startConnection() throws Exception{
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
    }
 
    public void sendMessage(String msg) throws Exception{
	System.out.println("Sending a msg from Cogni Client: "+ msg);
	out.println(msg);
    }
 
    public void stopConnection() throws Exception{
        out.close();
        clientSocket.close();
    }
}
