package crypto.communication;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;

import java.io.ObjectOutputStream;

import java.lang.String;
import java.net.ServerSocket;    
import crypto.HeadlessCryptoScanner;
import crypto.rules.CryptSLRule;

import java.util.concurrent.TimeUnit;


public class Server {

    private Socket clientSocket;
	private Socket agentClient; //for this server, but as client of agent
    private ServerSocket serverSocket;
	private DataOutputStream out;
	private BufferedReader in;
    private boolean oneRunDone = false;

	//for the agent
	//TODO better names
	DataOutputStream dOut;
	
    public void start(int port, List<CryptSLRule> rules) throws Exception{

		//first lets reorder them for the idea of more efficient compare order
		List<String> ruleNames = reorderSeeds(rules);
		
		serverSocket = new ServerSocket(port);
		System.out.println("");
		System.out.println("-----------------------------------");
		System.out.println("COGNISERVER: Starting up the server, waiting for client!");
		System.out.println("-----------------------------------");
		clientSocket = serverSocket.accept();
		out = new DataOutputStream(clientSocket.getOutputStream());
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		System.out.println("COGNISERVER: Client accepted");
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
        if ("END".equals(inputLine) || oneRunDone) {
			stop();
            break;
         }
		
				System.out.println("-----------------------------------");
		System.out.println("COGNISERVER: Recieved this input from client: "+inputLine);
		System.out.println("-----------------------------------");
		
	//TODO replace this with real msg protocol, prefer protobuf bc easy to manage
	String input = inputLine.trim();
	if("REQUESTSEEDS".equals(input)) {
		System.out.println("-----------------------------------");
        System.out.println("COGNISERVER: Recieved this input from client: "+inputLine);
        System.out.println("-----------------------------------");
		sendSeeds(ruleNames);
	} else if("INITANALYSIS".equals(input)){
		System.out.println("-----------------------------------");
        System.out.println("COGNISERVER: Recieved this input from client: "+inputLine);
        System.out.println("-----------------------------------");
		if((input = in.readLine()) != null){
			//temp trust in ourselves for passing classname as next line
			initAnalysis(input.trim());
			sendFix();
	    }else{
			System.out.println("COGNISERVER: Expected to receive classname next, received nothing instead.");
	    }
	}else{
	    //System.out.println("COGNISERVER: The message from client is unrecognized.");
	}
	    

	}
    }

	private void sendFix() throws Exception{

		System.out.println("-----------------------------------");
		System.out.println("COGNISERVER: sending the fix...");
		
		int port = 38401;
		agentClient = new Socket("127.0.0.1", port);
		dOut = new DataOutputStream(agentClient.getOutputStream());
		ObjectOutputStream objOut = new ObjectOutputStream(agentClient.getOutputStream());

		String redefdir= Paths.get("").toAbsolutePath().toString() + "/adapterOutput/";
		ArrayList<HashMap<Class, byte[]>> separatedRedefs = readDefsFromDir(redefdir);
		HashMap<Class, byte[]> newClasses = separatedRedefs.get(0);
		HashMap<Class, byte[]> patch = separatedRedefs.get(1);

		objOut.writeUTF("INITREDEFINITION");

		objOut.writeInt(newClasses.keySet().size());
        for(Class cls : newClasses.keySet()){
            System.out.println("COGNISERVER: sending this number of bytes to be read from array: "+ newClasses.get(cls).length);
            objOut.writeInt(newClasses.get(cls).length);
            objOut.write(newClasses.get(cls));
            objOut.writeUTF(cls.getName());
        }
		
		objOut.writeInt(patch.keySet().size());
		for(Class cls : patch.keySet()){
			System.out.println("COGNISERVER: sending this number of bytes to be read from array: "+ patch.get(cls).length);
			objOut.writeInt(patch.get(cls).length);
			objOut.write(patch.get(cls));
			objOut.writeObject(cls);
		}
		objOut.flush();
		System.out.println("COGNISERVER: fix sent.");
		System.out.println("-----------------------------------");

	}
	
	private ArrayList<HashMap<Class, byte[]>> readDefsFromDir(String strdir){
		//gather all class files in the arg dir
		ArrayList<HashMap<Class, byte[]>> alldefs = new ArrayList<HashMap<Class, byte[]>>();
		HashMap<Class, byte[]> cdfnewclasses = new HashMap<Class, byte[]>();
		HashMap<Class, byte[]> cdfredefs = new HashMap<Class, byte[]>();
		try {
			File dir = new File(strdir);
			File[] directoryListing = dir.listFiles();
			if (directoryListing != null) {
				for (File file : directoryListing) {
					if(file.toString().contains("class")){
						System.out.println("this was the absolute path: "+file.getAbsolutePath());
                        String classname = file.getAbsolutePath().replaceFirst(strdir , "");
                        System.out.println("Reading class: " + classname);
						byte[] classBytes= Files.readAllBytes(Paths.get(file.toString()));
                        System.out.println("using classname: "+ classname.replace(".class", ""));
                        Class cls = Class.forName(classname.replace(".class", ""));
						
						if(classname.contains("NewClass")){
							cdfnewclasses.put(cls, classBytes);
						} else{
							cdfredefs.put(cls, classBytes);
						}
					}
				}
			} else {
				System.out.println("Patch directory arg was insufficient: " + strdir);
			}
			
		} catch (Exception e){
			System.out.println("Some issue with reading classfiles: "+ e.getMessage());
			e.printStackTrace();
		}
		alldefs.add(cdfnewclasses);
		alldefs.add(cdfredefs);
		return alldefs;
	}
	
	private void sendSeeds(List<String> rules) throws Exception {
		//first send number of them
		System.out.println("COGNISERVER: sending this many seeds to read: "+ rules.size());
		TimeUnit.SECONDS.sleep(3);
		out.writeInt(rules.size());
		for (String rule : rules) {
			//the strings in sigs in the jit will contain / not . in package names
			out.writeUTF(rule.replace(".", "/"));
		}
		System.out.println("COGNISERVER: done sending seeds.");
		System.out.println("-----------------------------------");
	}

	//this is based on heurisitics
	//TODO refactor, this could be better
	private List<String> reorderSeeds(List<CryptSLRule> rules){
		//first put all class names into array
		List<String> theImportant = new ArrayList(Arrays.asList(new String[rules.size()]));

		theImportant.set(0, "java.security.MessageDigest");
		theImportant.set(1, "javax.crypto.Cipher");
		theImportant.set(2, "java.security.SecureRandom");
		theImportant.set(3, "java.security.Signature");

		int ogIndex = 0;
		int newIndex = 4;
		int end = rules.size();
		while (ogIndex != end) {
			switch( rules.get(ogIndex).getClassName() ){
			case "java.security.SecureRandom":
			case "java.security.MessageDigest":
			case "java.security.Signature":
			case "javax.crypto.Cipher":
				ogIndex++;
				continue;
			default:
				theImportant.set(newIndex, rules.get(ogIndex).getClassName());
				newIndex++;
				ogIndex++;
			}
		}
				
		return theImportant;
	}
	
	private void printClasses(List<CryptSLRule> rules){
        System.out.println("COGNISERVER: printing the classes that we have specs for");
		for (CryptSLRule rule : rules) {
			System.out.println(rule.getClassName());
        }
    }

    public void initAnalysis(String classname) throws Exception{

	System.out.println("COGNISERVER: Initing an analysis of: "+ classname);
	//TODO replace this hardcoded applicationCP
	//as is, can only run tests from the PanathonExamples dir, as located in root
	String[] commandLine = { "-sootCp=/root/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/images/j2sdk-image/jre/lib/jce.jar:/root/openj9cryptoReleases/Agent/:/root/openj9cryptoReleases/RedefExamples/target/classes/:/root/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/images/j2sdk-image/jre/lib/rt.jar", "-src-prec=cache", "--rulesDir=/root/CryptoAnalysis/CryptoAnalysis/src/main/resources/JavaCryptographicArchitecture/", "--arg-class="+classname};
	System.out.println("Running test for: "+ classname);
	System.out.println("Command line: "+ Arrays.toString(commandLine));

	System.out.println("COGNISERVER: STARTING ANALYSIS");
	System.out.println("------------------------------");
	HeadlessCryptoScanner.main(commandLine);
	oneRunDone = true;
    }

	private void runPatchAdapter(){
	}
	
    public void stop() throws Exception {
		System.out.println("COGNISERVER:  closing connection.");
		in.close();
        out.close();
		dOut.close();
        clientSocket.close();
        serverSocket.close();
		agentClient.close();
    }
}
