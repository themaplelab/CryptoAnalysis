package crypto;


import java.util.Iterator;
import crypto.communication.Server;
import java.lang.Exception;
import java.util.List;
import java.io.File;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;

import java.util.concurrent.TimeUnit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import crypto.analysis.CrySLRulesetSelector;
import crypto.rules.CrySLRule;
import differ.RelaxedParser;
import differ.SemanticOptions;

import crypto.analysis.CrySLRulesetSelector.RuleFormat;

public class TCPCryptoRunner {

    private static int port = 38401;
	private static List<CrySLRule> rules;
	private static CommandLine cogniOptions;
	private static CommandLine differOptions;
	
    public static void main(String[] args) throws Exception{

		RelaxedParser relaxedParser = new RelaxedParser();
        differOptions = relaxedParser.parse(new SemanticOptions(), args);
        List<String> options1 = new ArrayList<String>();
		relaxedParser.getLeftovers(options1);
		
		//for now, TODO consider if we really want the same set of
		//possible options as the HeadlessCryptoScanner
		CommandLineParser parser = new DefaultParser();
        cogniOptions = parser.parse(new HeadlessCryptoScannerOptions(), options1.toArray(new String[0]));

		//TODO add no option provided handling
		if (cogniOptions.hasOption("rulesDir")) {
            String resourcesPath = cogniOptions.getOptionValue("rulesDir");
            rules = CrySLRulesetSelector.makeFromPath(new File(resourcesPath), RuleFormat.SOURCE);
        }
		
		//will automatically setup as server, no checking of args
		Server server = new Server();

		
		ArrayList<String> patchNames = gatherPatchNamesFromFile(cogniOptions.getOptionValue("patchlist"));
		server.setCogniOptions(cogniOptions);
		server.setDifferOptions(differOptions);
		server.start(port, rules, patchNames);
    }

	//reads the classes that designate the patch, from a file. One class per line, fqn.                                         
    private static ArrayList<String> gatherPatchNamesFromFile(String patchFile){
		ArrayList<String> allNames = new ArrayList<String>();
        try{
            BufferedReader in = new BufferedReader(new FileReader(patchFile));
            String str;
            while((str = in.readLine()) != null){
                allNames.add(str.replace(".class", ""));
            }
        }catch(Exception e){
            System.out.println("Some issue accessing the classes that we have for the patch: "+ e.getMessage());
    }
        return allNames;
    }
	
}
