package com.leantech.server.io;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHandler {

    public static  StringBuilder getSite(String site){
        String line = "";
        StringBuilder stringBuilder = new StringBuilder();

        try{
            URL url = new URL(site);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            printHeader(site, httpURLConnection.getHeaderFields());

        } catch (Exception exception){
            System.out.println("Error on get site method: " + exception.getMessage());
        }
        return stringBuilder;
    }

    public static void printHeader(String site, Map<String, List<String>> headers){
        System.out.println("Headers for website " + site);
        headers.entrySet().forEach(entry -> System.out.println("Header entry: " + entry));
        System.out.println("End of headers");
        System.out.println();
    }

    public static void runServer(String host, int remoteport, int localport) throws IOException {
        // Creating a ServerSocket to listen for connections with
        ServerSocket serverSocket = new ServerSocket(localport);

        Pattern requestPattern = Pattern.compile("GET|CONNECT (.+) HTTP", Pattern.MULTILINE);
        while (true) {
            Socket client = null;
            try{
                client = serverSocket.accept();
                client.setSoTimeout(3000);
                BufferedReader proxyToClientBr = new BufferedReader(new InputStreamReader(client.getInputStream()));
                BufferedWriter proxyToClientBw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

                // Get request headers
                String requestText = proxyToClientBr.lines().findFirst().get();
                Matcher matcher = requestPattern.matcher(requestText);
                if (matcher.find()) {
                    requestText = matcher.group(1);
                    if(requestText != null) {
                        getSite("https://" + requestText);
                    }
                }
                String requestString = proxyToClientBr.readLine();
                System.out.println("Request Received: " + requestString);

                // Get the Request type
                String requestType = requestString.substring(0,requestString.indexOf(' '));

                // remove request type and space
                String urlString = requestString.substring(requestString.indexOf(' ')+1);

                // Remove everything past next space
                urlString = urlString.substring(0, urlString.indexOf(' '));

                // Add http:// if necessary to create a correct URL
                if(!urlString.substring(0,4).equals("http")){
                    String temp = "http://";
                    urlString = temp + urlString;
                }

                // Check request type
                if(requestType.equals("CONNECT")){
                    System.out.println("HTTPS Request for : " + urlString + "\n");
                    handleHTTPSRequest(client, proxyToClientBr, proxyToClientBw, urlString);
                } else{
                    System.out.println("HTTP GET for : " + urlString + "\n");
                    sendFileContentToClient(client, proxyToClientBr, proxyToClientBw, urlString);
                }
            } catch (Exception exception){

            } finally {
                client.close();
            }
        }
    }

    /**
     * Handles HTTPS requests between client and remote server
     * @param clientSocket: the socket holding the client's request
     * @param proxyToClientBr: a buffer to read a stream to send to the client
     * @param proxyToClientBw: a buffer to write the read stream to the client
     * @param urlString: the desired file to be transmitted over https
     */
    private static void handleHTTPSRequest(Socket clientSocket, BufferedReader proxyToClientBr, BufferedWriter proxyToClientBw, String urlString){
        // Extract the URL and port of remote
        String url = urlString.substring(7);
        String pieces[] = url.split(":");
        url = pieces[0];
        int port  = Integer.valueOf(pieces[1]);

        try{
            // Only first line of HTTPS request has been read at this point (CONNECT *)
            // Read (and throw away) the rest of the initial data on the stream
            for(int i=0; i<5; i++){
                proxyToClientBr.readLine();
            }

            // Get actual IP associated with this URL through DNS
            InetAddress address = InetAddress.getByName(url);

            // Open a socket to the remote server
            Socket proxyToServerSocket = new Socket(address, port);
            proxyToServerSocket.setSoTimeout(5000);

            // Send Connection established to the client
            String line = "HTTP/1.0 200 Connection established\r\n" +
                    "Proxy-Agent: ProxyServer/1.0\r\n" +
                    "\r\n";
            proxyToClientBw.write(line);
            proxyToClientBw.flush();

            // Client and Remote will both start sending data to proxy at this point
            // Proxy needs to asynchronously read data from each party and send it to the other party

            //Create a Buffered Writer between proxy and remote
            BufferedWriter proxyToServerBW = new BufferedWriter(new OutputStreamWriter(proxyToServerSocket.getOutputStream()));

            // Create Buffered Reader from proxy and remote
            BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerSocket.getInputStream()));

            // Create a new thread to listen to client and transmit to server
            ClientToServerHttpsTransmit clientToServerHttps =
                    new ClientToServerHttpsTransmit(clientSocket.getInputStream(), proxyToServerSocket.getOutputStream());

            Thread httpsClientToServer = new Thread(clientToServerHttps);
            httpsClientToServer.start();

            // Listen to remote server and relay to client
            try {
                byte[] buffer = new byte[4096];
                int read;
                do {
                    read = proxyToServerSocket.getInputStream().read(buffer);
                    if (read > 0) {
                        clientSocket.getOutputStream().write(buffer, 0, read);
                        if (proxyToServerSocket.getInputStream().available() < 1) {
                            clientSocket.getOutputStream().flush();
                        }
                    }
                } while (read >= 0);
            }
            catch (SocketTimeoutException e) {

            }
            catch (IOException e) {
                e.printStackTrace();
            }

            // Close Down Resources
            if(proxyToServerSocket != null){
                proxyToServerSocket.close();
            }

            if(proxyToServerBR != null){
                proxyToServerBR.close();
            }

            if(proxyToServerBW != null){
                proxyToServerBW.close();
            }

            if(proxyToClientBw != null){
                proxyToClientBw.close();
            }


        } catch (SocketTimeoutException exception) {
            String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
                    "User-Agent: ProxyServer/1.0\n" +
                    "\r\n";
            try{
                proxyToClientBw.write(line);
                proxyToClientBw.flush();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        catch (Exception e){
            System.out.println("Error on HTTPS : " + urlString );
            e.printStackTrace();
        }
    }

    /**
     * Sends the contents of the file specified by the urlString to the client
     * @param clientSocket: the socket holding the client's request
     * @param proxyToClientBr: a buffer to read a stream to send to the client
     * @param proxyToClientBw: a buffer to write the read stream to the client
     * @param urlString: URL of the file requested
     */
    private static void sendFileContentToClient(Socket clientSocket, BufferedReader proxyToClientBr, BufferedWriter proxyToClientBw, String urlString){

        try{
            // Compute a logical file name as per schema
            // This allows the files on stored on disk to resemble that of the URL it was taken from
            int fileExtensionIndex = urlString.lastIndexOf(".");
            String fileExtension;

            // Get the type of file
            fileExtension = urlString.substring(fileExtensionIndex, urlString.length());

            // Get the initial file name
            String fileName = urlString.substring(0,fileExtensionIndex);

            // Trim off http://www. as no need for it in file name
            fileName = fileName.substring(fileName.indexOf('.')+1);

            // Remove any illegal characters from file name
            fileName = fileName.replace("/", "__");
            fileName = fileName.replace('.','_');

            // Trailing / result in index.html of that directory being fetched
            if(fileExtension.contains("/")){
                fileExtension = fileExtension.replace("/", "__");
                fileExtension = fileExtension.replace('.','_');
                fileExtension += ".html";
            }

            fileName = fileName + fileExtension;

            // Check if file is an image
            if((fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||
                    fileExtension.contains(".jpeg") || fileExtension.contains(".gif")){
                // Create the URL
                URL remoteURL = new URL(urlString);
                BufferedImage image = ImageIO.read(remoteURL);

                if(image != null) {

                    // Send response code to client
                    String line = "HTTP/1.0 200 OK\n" +
                            "Proxy-agent: ProxyServer/1.0\n" +
                            "\r\n";
                    proxyToClientBw.write(line);
                    proxyToClientBw.flush();

                    // Send them the image data
                    ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());
                } else {
                    // No image received from remote server
                    System.out.println("Sending 404 to client as image wasn't received from server"
                            + fileName);
                    String error = "HTTP/1.0 404 NOT FOUND\n" +
                            "Proxy-agent: ProxyServer/1.0\n" +
                            "\r\n";
                    proxyToClientBw.write(error);
                    proxyToClientBw.flush();
                    return;
                }
            } else {
                // File is a text file

                // Create the URL
                URL remoteURL = new URL(urlString);
                // Create a connection to remote server
                HttpURLConnection proxyToServerCon = (HttpURLConnection)remoteURL.openConnection();

                proxyToServerCon.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                proxyToServerCon.setRequestProperty("Content-Language", "en-US");
                proxyToServerCon.setUseCaches(false);
                proxyToServerCon.setDoOutput(true);

                // Create Buffered Reader from remote Server to get returned data
                BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));

                // Send success code to client
                String line = "HTTP/1.0 200 OK\n" +
                        "Proxy-agent: ProxyServer/1.0\n" +
                        "\r\n";
                proxyToClientBw.write(line);

                // Read from input stream between proxy and remote server
                while((line = proxyToServerBR.readLine()) != null){
                    // Send on data to client
                    proxyToClientBw.write(line);

                }

                // Ensure all data is sent by this point
                proxyToClientBw.flush();

                // Close Down Resources
                if(proxyToServerBR != null){
                    proxyToServerBR.close();
                }
            }

            if(proxyToClientBw != null){
                proxyToClientBw.close();
            }
        } catch (UnknownHostException e){
            System.out.println("Unknown host: " + e.getMessage());
        } catch (Exception e){
            System.out.println("Error while sending data to client: " + e.getMessage());
        }
    }
}
