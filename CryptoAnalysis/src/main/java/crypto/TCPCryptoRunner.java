package crypto;

import crypto.communication.Server;
import java.io.IOException;
import java.lang.Exception;

public class TCPCryptoRunner{

    private static int port = 38400;
    
    public static void main(String[] args) throws Exception{

	//will automatically setup as server, no checking of args
	Server server = new Server();
	server.start(port);
    }

}
