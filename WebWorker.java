/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;
import java.util.StringTokenizer;
import java.util.Scanner;
import java.net.*;

public class WebWorker implements Runnable
{

private Socket socket;
private String file = ""; 
String mimeType;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      readHTTPRequest(is);

      mimeType= URLConnection.guessContentTypeFromName(file);
      System.out.println("File type is: " + mimeType);
      writeHTTPHeader(os,mimeType); 
   
      writeContent(os);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}
/**
* Read the HTTP request header.
**/

private void readHTTPRequest(InputStream is){
   String line = null;
   BufferedReader r = new BufferedReader(new InputStreamReader(is));

try{	
line = r.readLine();
} // end try

catch (Exception e){
   System.err.println("Request error: " + e);
} // end catch

StringTokenizer t = new StringTokenizer(line);
t.nextToken(); // skips "GET"
file = t.nextToken();   

   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         System.err.println("Request line: ("+line+")");
         if (line.length()==0) 
            break;
      }
      catch (Exception e) {
         System.err.println("Request error: " + e);
         break;
      }
   }
   return;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   try{
      FileReader fr = new FileReader(file.substring(1));
      BufferedReader br = new BufferedReader (fr);
   } // end try
   catch (FileNotFoundException e){
      System.err.println(e + " " + file + " not found");
      os.write("HTTP/1.1 404 Not Found\n".getBytes());
   } // end catch
   
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT")); 
   os.write("HTTP/1.1 200 Ok\n".getBytes());
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Evan's server\n".getBytes());
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;

}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/

private void writeContent(OutputStream os) throws Exception{
	
	// catches any image file
   if (URLConnection.guessContentTypeFromName(file).substring(0,5).contentEquals("image")){
	    byte[] byteArray = new byte[1024];
	    int i = 0;
	    FileInputStream in = new FileInputStream(file.substring(1)); // skips slash
	    // loop for byte output
	    while ((i = in.read(byteArray)) >= 0) {
	        os.write(byteArray, 0, i);
	    } // end while
   } // end if

   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT")); 

   System.out.println(file);
   file = file.substring(1); // removes forward-slash from string "file"
   String line;

   try{
      FileReader fr = new FileReader(file);
      BufferedReader br = new BufferedReader (fr); 

      // increments through file body
      while ((line = br.readLine()) != null){
         os.write(line.getBytes());
      
      if (line.equals("<cs371server>")){
         os.write("\nEvan's server\n".getBytes());
      } // end if

      if (line.equals("<cs371date>")){
         os.write((df.format(d)).getBytes());
      } // end if
      } // end while
   } // end try

   catch(FileNotFoundException e){
      System.err.println(file + " not found");
      os.write("<h1>404 Not Found</h1>".getBytes());
   } // end catch
} // end writeContent
} // end class
