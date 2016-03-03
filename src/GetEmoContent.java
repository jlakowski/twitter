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

import twitter4j.*;
import java.util.*;
import java.io.*;
import java.sql.*
;
/**
 * <p>This is a code example of Twitter4J Streaming API - sample method support.<br>
 * Usage: java twitter4j.examples.PrintSampleStream<br>
 * </p>
 *
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
public final class GetEmoContent {
    /**
     * Main entry of this application.
     *
     * @param args arguments doesn't take effect with this example
     * @throws TwitterException when Twitter service or network is unavailable
     */
	// I Guess I have a couple of different choices here.
	// I could create a dictionary at startup each time
	
	// I could also create a database that has these values stored in it
	// and adds new words to it each time.
	// I think loading the dictionary each time is fine.
	
	
	//creates dictionaries for storing word values
	public static Map<String, Integer> emodic = new HashMap<String, Integer>();
	public static Map<String, Integer> emomultdic = new HashMap<String, Integer>();
	public static Connection c = null;
	
	
	//start up function to initialize database and the emotional dictionaries
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
	
	//method to get an emotional score for each tweet
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
	
	//So the next thing to do is just get the english tweets and maybe spanish if
	//I can find a good spanish sentiment dictionary
	//May have to get back to the json format to get the time stamp to see how tweets 
	//change over the course of the night.
	// This would have been good to watch how sentiment changes over the course of this
	// Supertuesday evening. Oh well.
	
	public static void main(String[] args) throws TwitterException {
		startup();
		TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
		StatusListener listener = new StatusListener() {
			@Override
			public void onStatus(Status status) {
				//System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText());
				if(status.getLang().equals("en"))
				//if(status.getText().toLowerCase().contains("trump")|| status.getText().toLowerCase().contains("drumpf"))
				{
					
					int score = scoreTweet(status.getText());

					System.out.println("score: " + score + " language " + status.getLang() + " " + status.getText());

					//add this to the database
					try{
						Statement stmt = c.createStatement();
						String sql = "INSERT INTO tbl1(f1, f2) " + 
								"VALUES ('\"" + status.getText().replaceAll("'", "''") +"\"', '" + score + "')" ;

						stmt.executeUpdate(sql);
						stmt.close();
						//c.commit();
					}catch ( Exception e ) {
						System.err.println( e.getClass().getName() + ": " + e.getMessage() );
						System.exit(0);
					}            
				}
			}

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                //System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                //System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
            }

            @Override
            public void onScrubGeo(long userId, long upToStatusId) {
                //System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            @Override
            public void onStallWarning(StallWarning warning) {
               // System.out.println("Got stall warning:" + warning);
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
