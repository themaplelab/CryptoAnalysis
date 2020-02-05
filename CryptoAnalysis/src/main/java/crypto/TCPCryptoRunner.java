package crypto;

import crypto.communication.Server;
import java.lang.Exception;
import java.util.List;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import crypto.analysis.CrySLRulesetSelector;
import crypto.rules.CryptSLRule;

import differ.RelaxedParser;

public class TCPCryptoRunner {

    private static int port = 38400;
	private static List<CryptSLRule> rules;
	private static CommandLine options;
	
    public static void main(String[] args) throws Exception{


		RelaxedParser parser = new RelaxedParser();
        options = parser.parse(new SemanticOptions(), args);
        List<String> options1 = new ArrayList<String>();
		parser.getLeftovers(options1);
		
		//for now, TODO consider if we really want the same set of
		//possible options as the HeadlessCryptoScanner
		CommandLineParser parser = new DefaultParser();
        options = parser.parse(new HeadlessCryptoScannerOptions(), options1.toArray(new String[0]));

		//TODO add no option provided handling
		if (options.hasOption("rulesDir")) {
            String resourcesPath = options.getOptionValue("rulesDir");
            rules = CrySLRulesetSelector.makeFromPath(new File(resourcesPath));
        }
		
		//will automatically setup as server, no checking of args
		Server server = new Server();
		server.start(port, rules);
    }

}
