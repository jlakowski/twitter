/*
 * Copyright 2007 Yusuke Yamamoto
 * 
* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class StreamEmoContentTime {
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
                //line = line.replaceAll("\\s"," ");
            	String[] entry = line.split("\t");
            	//this is throwing an error for multipliers
            	//Also it can't handle two word phrases
            	if(entry[1].contains("*")){
            		emomultdic.put(entry[0], 2);
            	}else{
            		emodic.put(entry[0], Integer.parseInt(entry[entry.length -1]));
            	}
            	//System.out.println("Added " + entry[0]);
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
		for(int i=0; i< words.length; i++){
			if(emodic.containsKey(words[i])){
				score = score + emodic.get(words[i]);
			}
		}
		return score;
	}
	
    public static void main(String[] args) throws TwitterException {
    	startup();
    	final JSONParser parser = new JSONParser();
    	
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
						
						String time = (String)json.get("created_at");
						int emoscore = scoreTweet(text);
												
						try{
							Statement stmt = c.createStatement();
							String sql = "INSERT INTO tbl2(time, score, text) " + 
									"VALUES ('" + time +"','" + emoscore +"','" + text.replaceAll("'", "''")+"')" ;
							System.out.println("Score: " + emoscore +"\t"+ text);
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
