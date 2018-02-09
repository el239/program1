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
      writeHTTPHeader(os,"text/html");
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
// response.setContentType("text/html");
//   setContentType("image/*");
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

   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 

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

private void writeContent(OutputStream os) throws Exception
{

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

public static String findMime(String theFile)throws java.io.IOException, MalformedURLException{
    String type = null;
    URL thePath = new URL(theFile);
    URLConnection theConnection = null;
    theConnection = thePath.openConnection();
    type = theConnection.getContentType();
    return type;
  } // end findMime

public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Get the absolute path of the image
    ServletContext sc = getServletContext();
    String filename = sc.getRealPath("image.gif");

    // Get the MIME type of the image
    String mimeType = sc.getMimeType(filename);
    if (mimeType == null) {
        sc.log("Could not get MIME type of "+filename);
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
    }

    // Set content type
    resp.setContentType(mimeType);

    // Set content size
    File file = new File(filename);
    resp.setContentLength((int)file.length());

    // Open the file and output streams
    FileInputStream in = new FileInputStream(file);
    OutputStream out = resp.getOutputStream();

    // Copy the contents of the file to the output stream
    byte[] buf = new byte[1024];
    int count = 0;
    while ((count = in.read(buf)) >= 0) {
        out.write(buf, 0, count);
    }
    in.close();
    out.close();
}

} // end class
