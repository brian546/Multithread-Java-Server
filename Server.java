import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.net.InetAddress;
// Server class 
class Server { 
    public static void main(String[] args) 
    { 
        int port = 1111;
        
        // Server is set to listen on port 1111 at IP 127.0.0.1
        try (ServerSocket server = new ServerSocket(port,20,InetAddress.getByName("127.0.0.1"))){ 
            server.setReuseAddress(true); 
  
            // running infinite loop for getting 
            // client request 
            while (true) { 
  
                // socket object to receive incoming client 
                // requests 
                try {
                    Socket client = server.accept(); 

                    // create a new thread object 
                    ClientHandler clientSock = new ClientHandler(client); 

                    // This thread will handle the client 
                    // separately 
                    new Thread(clientSock).start(); 
                }
                catch (IOException e){
                    System.out.println("Accept failed:1111");
                    System.exit(1);
                }

            } 
        } 
        catch (IOException e) { 
            System.out.println("Could not listen on port:1111");
            System.exit(1);
        } 
    } 
  
    // ClientHandler class 
    private static class ClientHandler implements Runnable { 
        private final Socket clientSocket; 
        private static final String logFile = "server_log.txt";
        // Constructor 
        public ClientHandler(Socket socket) 
        { 
            this.clientSocket = socket; 
        } 
  
        public void run() 
        { 
            String requestMessageLine;
            String fileName;
            String method;
            String responseType;
            boolean parseDateErrorFound = false;
            long lastModifiedFromRequest = 0;
            try (                
                // get the inputstream of client 
                BufferedReader inFromClient = new BufferedReader( 
                new InputStreamReader( 
                    clientSocket.getInputStream()));
                    
                // get the outputstream of client 
                DataOutputStream outToClient = new DataOutputStream( 
                    clientSocket.getOutputStream());

            ){ 

                // read the first line of client reqeust: method ip http_version
                requestMessageLine = inFromClient.readLine();

                StringTokenizer tokenizedLine = new StringTokenizer(requestMessageLine);
                method = tokenizedLine.nextToken();
                fileName = tokenizedLine.nextToken();
                if (fileName.startsWith("/") == true )
                    fileName = fileName.substring(1);
                File file = new File(fileName);

                String line;

                // Search if there is "If-Modified-Since" header field in request message received and parse the value in the field into timestamp
                while (!(line = inFromClient.readLine()).isEmpty()) {
                    String[] res = line.split(": ",2);
                    if (res[0].equals("If-Modified-Since")) {
                        try{
                            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
            
                            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                            Date date = formatter.parse(res[1]);
                            lastModifiedFromRequest = date.getTime();
            
                        } catch (ParseException e) {
                            parseDateErrorFound = true;
                        }
                        break;
                    }
                }
                // server can only handle method GET or HEAD. If it receive request of other method, it will return 400 Bad Request.
                if ((method.equals("GET") || method.equals("HEAD"))){

                    // if the requested file doesn't exist, server will return 404 File Not Found
                    if (file.exists()){
                        int numOfBytes = (int) file.length();

                        // get file lastModified timestamp round to second
                        long lastModified = truncateMillis(file.lastModified());

                        // format lastModified timestamp in style of "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
                        String lastModifiedDate = formatDate(lastModified);
                        
                        // if the client doesn't have the up-to-date cached version or doesn't have the correct data format in "If-Modified-Since" field, server will return the object and return status 200 OK, otherwise, it won't send object and return status 304 Not Modified. 
                        if ((lastModified > lastModifiedFromRequest) || parseDateErrorFound) {
                            responseType = "200 OK";
                            outToClient.writeBytes("HTTP/1.0 200 OK\r\n");
                            outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");
                        } else {
                            responseType = "304 Not Modified";
                            outToClient.writeBytes("HTTP/1.0 304 Not Modified\r\n");
                        }

                        outToClient.writeBytes("Last-Modified: " + lastModifiedDate + "\r\n");
                        outToClient.writeBytes("\r\n");

                        // return data as message body if requested method is GET and client doesn't have the up-to-date cached version
                        if (method.equals("GET") & (lastModified > lastModifiedFromRequest)){
                            FileInputStream inFile = new FileInputStream(fileName);
                            byte[] fileInBytes = new byte[numOfBytes];
                            inFile.read(fileInBytes);
                            outToClient.write(fileInBytes, 0, numOfBytes);
                        }
                    } else {
                        responseType = "404 File Not Found";
                        outToClient.writeBytes("HTTP/1.0 404 File Not Found\r\n");
                        outToClient.writeBytes("\r\n");
                    }

                } else {
                    responseType = "400 Bad Request";
                    outToClient.writeBytes("HTTP/1.0 400 Bad Request\r\n");
                    outToClient.writeBytes("\r\n");
                }

                // Record client IP address, access time, requested file name and corresponding response type for each request received into server_log.txt
                // One request per line
                logRequest(clientSocket.getInetAddress().getHostAddress(),fileName,responseType);
            } 
            catch (IOException e) { 
                e.printStackTrace(); 
            }
        }

        private void logRequest(String clientIP, String fileName,String responseType) {
            try (BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFile, true))) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                logWriter.write(String.format("%s - %s - %s - %s", clientIP, timestamp, fileName, responseType));
                logWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private String formatDate(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            return sdf.format(new Date(timestamp));
        }

        private static long truncateMillis(long timestamp) {
            return (timestamp / 1000) * 1000;
        }

    }
}