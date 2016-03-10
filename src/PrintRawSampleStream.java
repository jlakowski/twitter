/*
 * J. Lakowski March 4, 2016
 * Modified the sample code. Lots of good stuff in here.
 * 
 * Streams the raw JSON Data from the tweeter sample stream
 * And then it sorts out the english tweets and saves the
 * time of the tweet, the uid, the tweet id, the score
 * and the text of the tweet in a squlite database
 * 
 */
//package twitter4j.examples.stream;

import twitter4j.RawStreamListener;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

import org.json.simple.JSONObject;

import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
/**
 * <p>This is a code example of Twitter4J Streaming API - sample method support.<br>
 * Usage: java twitter4j.examples.PrintRawSampleStream<br>
 * </p>
 *
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
public class PrintRawSampleStream {
    /**
     * Main entry of this application.
     *
     * @param args arguments doesn't take effect with this example
     * @throws TwitterException when Twitter service or network is unavailable
     */
	
	public static Map<String, Integer> emodic = new HashMap<String, Integer>();
	public static Map<String, Integer> emomultdic = new HashMap<String, Integer>();
	public static Connection c = null;
	
	public static void startup(){
		// The name of the file to open.
        String fileName = "emodict.txt";

        // This will reference one line at a time
        String line = null;
        int counter = 0;
        try {
            // FileReader reads text files in the default encoding.
            FileReader fileReader = 
                new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                //line = line.replaceAll("\\s","\t");
            	String[] entry = line.split("\t");
            	//this is throwing an error for multipliers
            	//Also it can't handle two word phrases
            	if(entry[1].contains("*")){
            		//System.out.println(entry[0]);
            		emomultdic.put(entry[0], 2);
            	}else{
            		emodic.put(entry[0], Integer.parseInt(entry[entry.length -1]));
            	}
            	System.out.println("Added " + entry[0]);
            	counter ++;
            }   

            // Always close files.
            bufferedReader.close();         
        }
        catch(FileNotFoundException ex) {
            ex.printStackTrace();            
        }
        catch(IOException ex) {
        	ex.printStackTrace();
            // Or we could just do this: 
            // ex.printStackTrace();
        }
        System.out.println("Added " + counter + " words to the dictionary");
        
        //Now set up the tweet database
        try{
        	Class.forName("org.sqlite.JDBC");
        	c = DriverManager.getConnection("jdbc:sqlite:tweets.sqlite");
        }catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Openedtweets database successfully!");
	}
	
	public static int scoreTweet(String statustext){
		int score = 0;
		String[] words = statustext.split(" ");
		short adjlast = 0;
		int multiplier = 0;
		for(int i=0; i< words.length; i++){
			//the multiplier isn't working yet
			if(emomultdic.containsKey(words[i]))
				multiplier = emomultdic.get(words[i]);
				adjlast = 1;
				
			if(emodic.containsKey(words[i])){
				score = score + emodic.get(words[i])*(1 + multiplier);
				multiplier = 0;
			}
			
		}
		return score;
	}
	
    public static void main(String[] args) throws TwitterException {
    	startup();
    	
    	TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
        RawStreamListener listener = new RawStreamListener() {
            @Override
            public void onMessage(String rawJSON) {
            	//System.out.println(rawJSON);
            	
            	try {
					JSONObject json = (JSONObject) new JSONParser().parse(rawJSON);
					String lang = (String)json.get("lang");
					if( lang.equals("en")){
						String text = (String)json.get("text");
						//to save space in the database (and my hard drive)
						//I think I'm only gonna look at the username?
						
						String time = (String)json.get("created_at");
						//int tsms = (Integer)json.get("timestamp_ms"); This still doesn't work, I don't know why
						
						//long tweetid = (Long)json.get("id");
						JSONObject user = (JSONObject)json.get("user");
						//long uid = (Long)user.get("id");
						String handle = (String)user.get("screen_name"); // get the handle
						int sentscore = scoreTweet(text);
						System.out.println(time + "\t" + sentscore + "\t@" + handle +"\t" +text);
												
						try{
							Statement stmt = c.createStatement();
							String sql = "INSERT INTO tbl3(time, sentscore, handle, text) " + 
									"VALUES ('" + time +"','" + sentscore +"','@" + handle +"','" + text.replaceAll("'", "''")+"')" ;
							//System.out.println("Score: " + sentscore +"\t"+ text);
							stmt.executeUpdate(sql);
							stmt.close();
							//c.commit();
						}catch ( Exception e ) {
							System.err.println( e.getClass().getName() + ": " + e.getMessage() );
							System.exit(0);
						}   
												
					}
					

				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	//System.out.println(rawJSON);
            }

            @Override
            public void onException(Exception ex) {
               
            	ex.printStackTrace();
            }	
        };
        twitterStream.addListener(listener);
        twitterStream.sample();
    }
}
