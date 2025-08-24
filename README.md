# Project Multi-Threaded Web Server

To run the project, you need to compile and run Server.java

1. Compile `Server.java`
javac Server.java

2. Run the Compiled Java Program
java Server

When the server is running, you are able to access helloworld.html with link "127.0.0.1:1111/helloworld.html". Ther server can response with status "200 OK", "304 Not Modified", "400 Bad Request", and "404 File Not Found", depending on message received. 

For example, if receiving messages with a method other than "GET" or "HEAD", it will respond with status 400; if the host searches for file "hello.html", which doesn't exist, the server will return 404 as response; if the host has already got the up-to-date "helloworld.html" file, the server will not send data again but will respond with status 304.

Moreover, each request will be logged into server_log.txt, including its host IP address, access time, requested file name, and corresponding response type.