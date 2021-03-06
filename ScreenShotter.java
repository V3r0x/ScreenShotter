import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.imageio.ImageIO;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;

import com.jcraft.jsch.*;


public class ScreenShotter {
	
	// TODO http://stackoverflow.com/questions/5953525/run-java-application-at-windows-startup
	// TODO add GUI
	// TODO add other upload clients
	
	/* 			NECESSARY CONFIGURATIONS
	 * * * * * * * * * * * * * * * * * * * * * * * * *
	 * Configure SFTP account data for file upload
	 * Login must be right, does not working otherwise!
	 */
	
	// TODO Java Properties benutzen um die sftp daten zu uebergeben.

	/*
	 * Play a sound when uploaded and copied to clipboard?
	 */
	
	final static boolean playTune = true;
	
	/*
	 * Trigger keys for capturing a screenshot are defined
	 * here. Actual keys are 37 (left arrow) and 39
	 * which is the right arrow.
	 */

	final static int triggerKey1 = 37; // left  arrow
	final static int triggerKey2 = 39; // right arrow
	
	/* 
	 * Google URL Shortener API Key must be declared here.
	 * Create API Key here: https://code.google.com/apis/console
	 * and get the key here: https://code.google.com/apis/console#access
	 */
	
	final static String googUrl = "https://www.googleapis.com/urlshortener/v1/url?shortUrl=http://goo.gl/fbsS&key=AIzaSyDXmdjeMFOzIjz8n8AS0d031N6LQ86o2Ts";

	
	public static void main(String[] args) {
		startKeyListener(triggerKey1, triggerKey2);
	}
	
	
	/*
	 * Starts JNativeListener to listen to
	 * background keypresses on a system-wide level
	 * calls triggerActivated() if passed keys key1 and key2
	 * are pressed simultaneously.
	 */
	public static void startKeyListener(int key1, int key2) {
		   try {
	               GlobalScreen.registerNativeHook();
		   }
	       catch (NativeHookException ex) {
	               System.err.println("There was a problem registering the native hook.");
	               System.err.println(ex.getMessage());

	               System.exit(1);
	       }

	       /* Create KeyListener via JNativeHook */
	       GlobalScreen.getInstance().addNativeKeyListener(new GlobalKeyListener(key1,key2));
	}
	
	
	/*
	 * Trigger (keys got presed) got activated, perform action e.g.
	 * make screenshot, afterwards upload.
	 */
	public static void triggerActivated() throws Exception {
		// Zeitmessung start
		long start = System.currentTimeMillis(); // LOL
		
		linkToClipboard("Sorry, the program needs some time to upload. Try to paste link again in some seconds...");
		
		String filename = makeAndSaveScreenShot();
		
		Uploader upl = new SFTPUploader();
		
		String httplink = upl.uploadScreenshot(filename);
		
		String shorted = shortenURL(httplink);
		
		linkToClipboard(shorted);
		
		if(playTune) playSound();
		
		// Zeitmessung ende
		long end = System.currentTimeMillis();
		long finalezeit = end - start;
		System.out.println("Dauer: " + finalezeit + "ms");
	}




	public static String makeAndSaveScreenShot() throws AWTException, IOException {
			SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");
	 
	        Calendar now = Calendar.getInstance();
	        Robot robot = new Robot();
	        
	        /* put together filename*/
	        String filename = "screenshot-"+formatter.format(now.getTime());
	        filename = encrypt(filename)+".png";
	        
	        try {
	        	BufferedImage screenShot = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
	        	/* add ./ to filename to specify save path */
	        	ImageIO.write(screenShot, "PNG", new File("./"+filename));
	        } catch(Exception e) {
	        	e.getMessage();
	        	System.err.println("Problems occured while processing the screenshot");
	        }
	        
	        System.out.println(formatter.format(now.getTime()));
	 
	        return filename;
	}
	
	
	  public static String encrypt(String source) {
		  String md5 = null;
		  try {
		      MessageDigest mdEnc = MessageDigest.getInstance("MD5"); // Encryption algorithm
		      mdEnc.update(source.getBytes(), 0, source.length());
		      md5 = new BigInteger(1, mdEnc.digest()).toString(16); // Encrypted string
		  } catch (Exception ex) {
		      return null;
		  }
		  return md5;
	  }
	

	
	
	
	/*
	 * Copy the generated link into the clipboard
	 */
	private static void linkToClipboard(String httplink) {
		 Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
	               new StringSelection(httplink), null
	          );
		 System.out.println("Link copied to clipboard.");
	}
	
	
	
	/*
	 * Stolen from: http://www.glodde.com/blog/default.aspx?id=51&t=Java-Use-googl-URL-shorten-from-Java
	 */
	private static String shortenURL(String longUrl) {
		String shortUrl = "";

	    try {
	        URLConnection conn = new URL(googUrl).openConnection();
	        conn.setDoOutput(true);
	        conn.setRequestProperty("Content-Type", "application/json");
	        OutputStreamWriter wr = 
	                     new OutputStreamWriter(conn.getOutputStream());
	        wr.write("{\"longUrl\":\"" + longUrl + "\"}");
	        wr.flush();

	        // Get the response
	        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String line;

	        while ((line = rd.readLine()) != null) {
	            if (line.indexOf("id") > -1) {
	                // I'm sure there's a more elegant way of parsing
	                // the JSON response, but this is quick/dirty =)
	                shortUrl = line.substring(8, line.length() - 2);
	                break;
	            }
	        }

	        wr.close();
	        rd.close();
	    }
	    catch (MalformedURLException ex) {
	    	ex.getMessage();
	    } catch (IOException ex) {
	    	ex.getMessage();
	    }

	    System.out.println("Short URL: " + shortUrl);
	    
	    return shortUrl;
	}
	
	
	/* 
	 * Play a sound, when link is copied to clipboard successfully
	 */
	private static void playSound() {
		Toolkit.getDefaultToolkit().beep();
	}
	

}
