package crypto.communication;

import java.lang.NoSuchMethodException;
import java.lang.ClassNotFoundException;
import org.apache.commons.cli.ParseException;
import java.util.Arrays;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.String;
import java.net.ServerSocket;    
import java.io.IOException;
import crypto.HeadlessCryptoScanner;

public class Server {

    private Socket clientSocket;
    private ServerSocket serverSocket;
    private PrintWriter out;
    private BufferedReader in;

    //    public void start(int port) throws IOException, ParseException, ClassNotFoundException, NoSuchMethodException{
    public void start(int port) throws Exception{
	serverSocket = new ServerSocket(port);
        clientSocket = serverSocket.accept();
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

	System.out.println("Client accepted");
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
        if ("END".equals(inputLine)) {
            System.out.println("good bye");
            break;
         }
	System.out.println("Recieved this input from client: "+inputLine.trim());

	//TODO replace this with real msg protocol, prefer protobuf bc easy to manage
	if("INITANALYSIS".equals(inputLine.trim())){
	    if((inputLine = in.readLine()) != null){
		//temp trust in ourselves for passing classname as next line
		initAnalysis(inputLine.trim());
	    }
	}

	}
    }

    //    public void initAnalysis(String classname) throws ParseException, ClassNotFoundException, NoSuchMethodException{
    public void initAnalysis(String classname) throws Exception{
    //TODO replace this hardcoded applicationCP
	//as is, can only run tests from the PanathonExamples dir, as located in root
	String[] commandLine = { "-src-prec=cache", "--rulesDir=/root/CryptoAnalysis/CryptoAnalysis/src/main/resources/JavaCryptographicArchitecture/", "--applicationCp=/root/PanathonExampleMaterials/exBin"};
	System.out.println("Running test for: "+ classname);
	System.out.println("Command line: "+ Arrays.toString(commandLine));

	HeadlessCryptoScanner.main(commandLine);
    }
}
