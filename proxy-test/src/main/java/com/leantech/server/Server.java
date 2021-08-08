package com.leantech.server;

import com.leantech.server.config.Configuration;
import com.leantech.server.config.ConfigurationManager;
import com.leantech.server.io.RequestHandler;

public class Server {

    public static void main(String[] args) {
        System.out.println("Starting server...");

        ConfigurationManager.getInstance().loadConfigurationFile("src/main/resources/http.json");
        Configuration configuration = ConfigurationManager.getInstance().getCurrentConfiguration();

        System.out.println("Local Port in use: " + configuration.getLocalPort());

        try {
            String host = "Proxy Server";

            // Start running the server
            RequestHandler.runServer(host, configuration.getRemotePort(), configuration.getLocalPort());
        }
        catch (Exception exception)
        {
            System.out.println("Error while running the proxy server: " + exception.getMessage());
        }
    }
}
