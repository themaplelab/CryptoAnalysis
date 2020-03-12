package crypto.communication;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import org.apache.commons.cli.CommandLine;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;

import java.io.ObjectOutputStream;

import java.lang.String;
import java.net.ServerSocket;    
import crypto.HeadlessCryptoScanner;
import crypto.rules.CryptSLRule;

import java.util.concurrent.TimeUnit;

import differ.SemanticDiffer;

import soot.asm.CacheClassProvider;
import soot.Transform;
import soot.PackManager;
import soot.options.Options;

public class Server {

    private Socket clientSocket;
	private Socket agentClient; //for this server, but as client of agent
    private ServerSocket serverSocket;
	private DataOutputStream out;
	private BufferedReader in;
	//private DataInputStream in;
	private boolean oneRunDone = false;

	//for the agent
	//TODO better names
	DataOutputStream dOut;


	private CommandLine cogniOptions;
    private CommandLine differOptions;
	private ArrayList<String> patchNames;

	
	private List<String> patchGeneratedClassesRedefs = new ArrayList<String>();
	private List<String> patchGeneratedClassesHosts = new ArrayList<String>();

	private static String redefdir= Paths.get("").toAbsolutePath().toString() + "/adapterOutput/";


	private ArrayList<String> finishedNames = new ArrayList<String>();
	
	public void setCogniOptions(CommandLine options){
		cogniOptions = options;
	}

	public void setDifferOptions(CommandLine options){
		differOptions = options;
    }
	
    public void start(int port, List<CryptSLRule> rules, ArrayList<String> patches) throws Exception{

		patchNames = patches; 
		System.out.println("COGNISERVER: using list of patches that are currently available: "+ patchNames);
		
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
		//in = new DataInputStream(clientSocket.getInputStream());
		
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
		//this is a terrible protocol usage, true
		int len = 6;
		char[] cbuf = new char[len];
		System.out.println("COGNISERVER: cbuf return : "+ in.read(cbuf, 0 , len));
		System.out.println("COGNISERVER: this is the cbuf "+ new String(cbuf));
		// todo refactor the above, someday.
		if((input = in.readLine()) != null){
			//temp trust in ourselves for passing classname as next line
			String mainClass = input.trim().replaceAll("\\/", ".");
			System.out.println("COGNISERVER: Recieved this input from client as classname: "+mainClass);
			if(patchNames.contains(mainClass) && !finishedNames.contains(mainClass)){
				boolean foundErrors = initAnalysis(mainClass, true);
				if(foundErrors){
					runPatchAdapter(mainClass);
					sendFix();
					finishedNames.add(mainClass); //just for now we want to avoid doing the same one twice
					TimeUnit.SECONDS.sleep(60); //also just for eval
					initAnalysis(mainClass, false); //this call is just for EVAL purposes
				}
			}
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

		ArrayList<HashMap<Class, byte[]>> separatedRedefs = readDefsFromDir(redefdir);
		HashMap<Class, byte[]> newClasses = separatedRedefs.get(0);
		HashMap<Class, byte[]> patch = separatedRedefs.get(1);

		System.out.println("COGNISERVER: these are the new classes: "+newClasses);
		System.out.println("COGNISERVER: these are the patch classes: "+patch);
		
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

			for(String fqncls : patchGeneratedClassesRedefs){
				System.out.println("Reading class: " + fqncls);
				String fullClassname = strdir+fqncls.replace(".","/")+".class";
				System.out.println("Using full path: "+ fullClassname);
				byte[] classBytes= Files.readAllBytes(Paths.get(fullClassname));
				System.out.println("using classname: "+ fqncls);
				Class cls = Class.forName(fqncls);
				cdfredefs.put(cls, classBytes);
			}

			for(String fqncls : patchGeneratedClassesHosts){
                System.out.println("Reading class: " + fqncls);
                String fullClassname = strdir+fqncls.replace(".","/")+".class";
                System.out.println("Using full path: "+ fullClassname);
                byte[] classBytes= Files.readAllBytes(Paths.get(fullClassname));
                System.out.println("using classname: "+ fqncls);
                Class cls = Class.forName(fqncls);
                cdfnewclasses.put(cls, classBytes);
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

    public boolean initAnalysis(String classname, boolean useSCCForAppClass) throws Exception{
 
		String rulesDir = null;
		String sootCp = null;
		
		System.out.println("COGNISERVER: Initing an analysis of: "+ classname);
		//TODO replace this hardcoded applicationCP
		//as is, can only run tests from the PanathonExamples dir, as located in root
		if (cogniOptions.hasOption("rulesDir")) {
			rulesDir = cogniOptions.getOptionValue("rulesDir");
		} else {
			rulesDir = "/root/CryptoAnalysis/CryptoAnalysis/src/main/resources/JavaCryptographicArchitecture/";
		}
		if(cogniOptions.hasOption("sootCp")){
			sootCp = cogniOptions.getOptionValue("sootCp");
		} else {
			sootCp = "/root/openj9cryptoReleases/RedefExamples/target/classes/:/root/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/images/j2sdk-image/jre/lib/jce.jar:/root/openj9cryptoReleases/Agent/:/root/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/images/j2sdk-image/jre/lib/rt.jar";
		}
		
		if(useSCCForAppClass){
			//the path to passing this as a conventional param is long, cutting it short in a bad way
			CacheClassProvider.setTestClassUrl(sootCp.split(":")[0]);
			System.out.println("COGNISERVER: using this as the testclassurl: "+ sootCp.split(":")[0]);
		}else{
			//want to analyze the patch version, not scc version
			sootCp = redefdir + ":" + sootCp ;
			CacheClassProvider.setTestClassUrl("");
			System.out.println("COGNISERVER: using this as the testclassurl: "+ "");
		}

		String[] commandLine = { "-sootCp="+sootCp, "-src-prec=cache", "--rulesDir="+rulesDir, "--arg-class="+classname};
        System.out.println("Running test for: "+ classname);
        System.out.println("Command line: "+ Arrays.toString(commandLine));

		System.out.println("COGNISERVER: STARTING ANALYSIS");
        System.out.println("------------------------------");
		
		HeadlessCryptoScanner.main(commandLine);
		if(HeadlessCryptoScanner.foundErrors()){
			System.out.println("COGNISERVER: misuse(s) detected!");
			return true;
		}
		oneRunDone = true;
		return false;
    }
	
	private void runPatchAdapter(String mainClass){
		String cpplaceholder = null;
		String mainclassplaceholder = null;
		String redefdirplaceholder = null;
		/*if(cogniOptions.hasOption("sootCp")){
			cpplaceholder = cogniOptions.getOptionValue("sootCp");
			}*/
		if(differOptions.hasOption("differClasspath")){
			cpplaceholder = differOptions.getOptionValue("differClasspath");
		}
		
		if(differOptions.hasOption("redefcp")){
			redefdirplaceholder = differOptions.getOptionValue("redefcp");
		}
		String[] differArgs = {"-cp", cpplaceholder, "-w", "-firstDest", Paths.get("").toAbsolutePath().toString()+"/renamedOriginals", "-altDest", "adapterOutput", "-redefcp", redefdirplaceholder, "-runRename", "true", "-mainClass", mainClass, "-originalclasslist", Paths.get("").toAbsolutePath().toString() + "/" + mainClass + ".originalclasses.out", "Example"};
		try{
			System.out.println("COGNISERVER: these are args to semantic differ from cogni: "+Arrays.toString(differArgs));
			//have to fix some settings in soot from cogni run
			Options.v().set_output_format(Options.output_format_class);
			//Transform myTransform = PackManager.v().getPack("jap").get("jap.myTransform");
			Transform ifds = PackManager.v().getPack("wjtp").get("wjtp.ifds");
			PackManager.v().getPack("jap").remove("jap.myTransform");
			PackManager.v().getPack("wjtp").remove("wjtp.ifds");
			SemanticDiffer.main(differArgs);
			patchGeneratedClassesRedefs = SemanticDiffer.getGeneratedClassesRedefs();
			patchGeneratedClassesHosts = SemanticDiffer.getGeneratedClassesHosts();
			Options.v().set_output_format(Options.output_format_none);
			//PackManager.v().getPack("jap").add(myTransform);
			PackManager.v().getPack("wjtp").add(ifds);
		} catch (Exception e){
			System.out.println("COGNISERVER: could not invoke semantic differ.");
			 e.printStackTrace();
		}
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
