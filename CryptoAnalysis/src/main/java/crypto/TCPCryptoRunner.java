package crypto;

import java.util.Iterator;
import crypto.communication.Server;
import java.lang.Exception;
import java.util.List;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import crypto.analysis.CrySLRulesetSelector;
import crypto.rules.CryptSLRule;
import differ.RelaxedParser;
import differ.SemanticOptions;

public class TCPCryptoRunner {

    private static int port = 38400;
	private static List<CryptSLRule> rules;
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
            rules = CrySLRulesetSelector.makeFromPath(new File(resourcesPath));
        }
		
		//will automatically setup as server, no checking of args
		Server server = new Server();

		Iterator it = cogniOptions.iterator();
		while(it.hasNext()){
			System.out.println("cognioption: "+ it.next());
		}
		Iterator it2 = differOptions.iterator();
        while(it2.hasNext()){
	    System.out.println("differoption: "+ it2.next());
		}
		
		server.setCogniOptions(cogniOptions);
		server.setDifferOptions(differOptions);
		server.start(port, rules);
    }

}
