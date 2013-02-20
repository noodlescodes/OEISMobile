package com.noodlescodes.oeismobile;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class OEIS extends Activity {

	Button search_button;
	EditText search_text;
	WebView webView;
	String html, webAddress, error;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oeis_main);
        
        search_button = (Button) findViewById(R.id.search_button);
        search_text = (EditText) findViewById(R.id.search_text);
        webView = (WebView) findViewById(R.id.webView);
        
        setup(); //Made a distinct method for setup due to possible future expansions.
    }
    
    @Override
    public void onPause()
    {
    	super.onPause();
    }
    
    private void setup() {
    	webView.loadUrl("file:///android_asset/default_page.html");
    	
    	search_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(search_text.getText().toString().length() == 0) {
					Toast.makeText(OEIS.this, "Please enter a sequence to search.", Toast.LENGTH_LONG).show();
				}
				else {
					if(parse(search_text.getText().toString())) {
    					new loadPage().execute();
    				}
				}
			}
		});
    	
    	search_text.setOnEditorActionListener(new OnEditorActionListener() {
        	@Override
        	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        		if(event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && event.getAction() == KeyEvent.ACTION_UP){
        			if(search_text.getText().toString().length() > 0) {
        				if(parse(search_text.getText().toString())) {
        					new loadPage().execute();
        				}
        			}
        			else {
        				Toast.makeText(getBaseContext(), "Please enter a sequence to search.", Toast.LENGTH_LONG).show();
        			}
        		}
        		return true;
        	}
        });
    }
    
    private boolean parse(String sequence) {
    	boolean legal = true;
    	
    	//hides the soft keyboard when a sequence is correctly entered.
    	InputMethodManager mgr = (InputMethodManager)getSystemService(OEIS.INPUT_METHOD_SERVICE);
    	mgr.hideSoftInputFromWindow(search_text.getWindowToken(), 0);

    	webAddress = "http://oeis.org/search?q="; //start constructing the webaddress to be downloaded
    	
    	//Construct the rest of the webaddress to be downloaded
    	for(int i = 0; i < sequence.length(); i++) { //parse the string entered. Currently only numbers, whitespace and ',' are allowed.
    		if(Character.isDigit(sequence.charAt(i))) {
				int j = 0;
				while(Character.isDigit(sequence.charAt(i + j))) {
					j++;
					//Don't want to extend past the length of the string
					if(i + j == sequence.length()) {
						break;
					}
				}
				webAddress += sequence.substring(i, i + j);
				i += j - 1;
    		}
    		else if(Character.isSpaceChar(sequence.charAt(i))) {
    			webAddress += "+";
    		}
    		else if(sequence.charAt(i) == ',') {
    			webAddress += "%2C";
    		}
    		else {
    			String error = "<html><body style=\"color:#FFFFFF;background-color:#000000\">Sorry, you've entered an incorrect character. Please fix the error and try again.</body></html>";
    			webView.loadData(error, "text/html", "UTF-8");
    			legal = false;
    		}
    	}
    	
    	return legal;
    }
    
    protected class loadPage extends AsyncTask<Void, Void, Void> {
    	ProgressDialog progressDialog;
    	Document d;
    	Elements tables;
    	boolean succeeded = true;
    	
    	@Override
    	protected void onPreExecute() {
    		progressDialog = ProgressDialog.show(OEIS.this, "Loading...", "Please wait"); 
    	}
    	
		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				d = Jsoup.connect(webAddress).timeout(0).get();
				tables = d.select("center > table > tbody > tr > td"); //This sets table.get(0) to the element just above all the required data
			} catch(SocketTimeoutException e) {
				error = "<html><body style=\"color:#FFFFFF;background-color:#000000\">Sorry, there was a time out error. This could be an indication that the number of sequences that are trying to be obtained is very large. If it's possible, please try to refine the sequence and try again.</body></html>";
				succeeded = false; //if the downloading failed, then it will throw an exception.
			} catch(IOException e) {
				error = "<html><body style=\"color:#FFFFFF;background-color:#000000\">Sorry, there was an IO error. Please try again soon.</body></html>";
				succeeded = false; //if the downloading failed, then it will throw an exception.
			} catch(Exception e) {
				error = "<html><body style=\"color:#FFFFFF;background-color:#000000\">Sorry, something went wrong, please try again.</body></html>";
				succeeded = false; //if the downloading failed, then it will throw an exception.
			}
			
 			return null;
		}
		
		@Override
		protected void onPostExecute(Void args0) {
			try {
				progressDialog.dismiss();
			} catch(Exception e) {
				//do nothing for now
			}
			
			if(!succeeded) { //downloading the page failed if !succeeded == true
    			webView.loadData(error, "text/html", "UTF-8");
			}
			else {				
				html = "<html><body>";
				
				for(int j = 5; j < tables.get(0).childNodeSize(); j++) { //Not really sure what's going on with tables.get(0). It's not returning what I expect, but this works.
					try {
						Element t;
						Elements temp;
						String str = "";
						t = tables.get(0).child(j);
						
						//As it stands, this is a bad implementation.
						//There is not comments, references and links
						//on every sequence, so it is currently
						//grabbing things that are not in those sections.
						//Improving on this is probably the highest priority.
						
						//Remove all the links that aren't in the "links" section. For now.
					
						t.select("font").remove(); //cleaning up the JSON to make things work easier.
						str += t.select("td > a").get(0).toString() + "<hr>"; //gets the sequence number
						str += t.select("tr > td").get(5).toString() + "<hr>"; //gets the description of the sequence
						str += t.select("tr").get(4).toString() + "<hr>"; //gets the sequence
						
						//construct the "Comments" section. 
						str += "<hr>Comments:<br />";
						temp = t.select("tbody > tr").get(6).select("tbody > tr").get(1).select("td > p"); //selects all the <p></p> tags inside a table
						for(int i = 0; i < temp.size(); i++) { //iterate over all the <p></p> tags and combine them into the html that will be loaded.
							str += temp.get(i).toString() + "<hr>";
						}
						
						//constructs the "References" section.
						str += "<hr>References:<br />";
						temp = t.select("tbody > tr").get(9).select("td").get(2).select("td > p"); //selects all the <p></p> tags inside a table
						for(int i = 0; i < temp.size(); i++) { //iterate over all the <p></p> tags and combine them into the html that will be loaded.
							str += temp.get(i).toString() + "<hr>";
						}
						str = str.replaceAll("<a href.*?>", "<p>");
						str = str.replaceAll("</a>", "</p>");
						html += str;
						
						//constructs the "Links" section.
						html += "<hr>Links:<br />";
						temp = t.select("tbody > tr").get(10).select("td").get(2).select("td > p"); //selects all the <p></p> tags inside a table
						for(int i = 0; i < temp.size(); i++) { //iterate over all the <p></p> tags and combine them into the html that will be loaded.
							html += temp.get(i).toString() + "<hr>";
						}
						html += "<hr><hr>";
					} catch(Exception e) { //out of bounds errors occur when there are no more elements to capture
						break;
					}
				}
				
				html += "</body></html>"; //Close all the tags!
				
				html = html.replaceAll("width=\"[0-9]+%?\"", ""); //remove all predefined widths to make sure the WebView handles all the widths.
				
				//used for debugging purposes
				/*try {
					FileOutputStream out = c.openFileOutput("html.txt", 0);
					out.write(html.getBytes());
					out.close();
				} catch(Exception e) {}*/
				
				webView.loadDataWithBaseURL(webAddress, html, "text/html", "UTF-8", null); //load what has been constructed, hope it's right.
			}
		}
    }
}