package com.brightcove.example.androidvideodemo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup;

import com.brightcove.ima.GoogleIMAComponent;
import com.brightcove.ima.GoogleIMAEventType;
import com.brightcove.player.event.Event;
import com.brightcove.player.event.EventEmitter;
import com.brightcove.player.event.EventListener;
import com.brightcove.player.event.EventType;
import com.brightcove.player.model.CuePoint;
import com.brightcove.player.model.CuePoint.PositionType;
import com.brightcove.player.model.Video;
import com.brightcove.player.view.BrightcoveVideoView;
import com.brightcove.sdk.sample.googleima.R;
import com.google.ads.interactivemedia.api.AdsManager.AdEvent;
import com.google.ads.interactivemedia.api.AdsManager.AdEventType;
import com.google.ads.interactivemedia.api.CompanionAdSlot;
import com.google.ads.interactivemedia.api.SimpleAdsRequest;
import com.google.ads.interactivemedia.api.SimpleAdsRequest.AdType;

public class MainActivity extends Activity {
	//Tag used for logging
	//private static final String TAG = "BrightcoveAndroidIMASample";
	
	//URL of the video we are playing
	private static final String VIDEO_URL = "http://cf9c36303a9981e3e8cc-31a5eb2af178214dc2ca6ce50f208bb5.r97.cf1.rackcdn.com/lucy_gets_shot_1280x720.mp4";

	//String array of ad URLS
    private static final String[] googleAds = {
            "http://pubads.g.doubleclick.net/gampad/ads?" +
            "sz=400x300&iu=%2F6062%2Fhanna_MA_group%2Fvideo_comp_app&ciu_szs=&impl=s&gdfp_req=1&" +
            "env=vp&output=xml_vast2&unviewed_position_start=1&m_ast=vast&url=[referrer_url]&" +
            "correlator=[timestamp]",
            "http://ad.doubleclick.net/pfadx/CABdemoSite;kw=acb;sz=728x90;ord=29078349023890482394823;dcmt=text/xml",
            "http://pubads.g.doubleclick.net/gampad/ads?sz=400x300&iu=%2F6062%2Fhanna_MA_group%2Fwrapper_with_comp&ciu_szs=728x90&impl=s&gdfp_req=1&env=vp&output=xml_vast2&unviewed_position_start=1&m_ast=vast&url=[referrer_url]&correlator=[timestamp]",
        };
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Get the video view
		final BrightcoveVideoView videoView = (BrightcoveVideoView) findViewById(R.id.video_view);
		
		//Initialize the Brightcove Google IMA Component 
		GoogleIMAComponent comp = new GoogleIMAComponent(videoView, videoView.getEventEmitter());
		
		//Setup our event listeners
		setupEventListeners(videoView);
		
		//Add a video!
		videoView.add(Video.createVideo(VIDEO_URL));
	}

	private void setupEventListeners(final BrightcoveVideoView videoView) {

		/*
		 * DID_SET_VIDEO is a good place to set up cue points.
		 * For advertising, we have hard coded some advertising URLs from Doubleclick
		 * 
		 * To start the advertising process, we need to create cue points that represent
		 * where the advertising exists, in this case we are going to create a 
		 * pre/mid/post roll
		 */
		videoView.getEventEmitter().on(EventType.DID_SET_VIDEO, new EventListener(){
		    
		    public void processEvent(Event event) {
		    	//Reference to eventEmitter
		    	EventEmitter eventEmitter = videoView.getEventEmitter();
		    	
		    	final String cuePointType = "ad";
		    	
		    	//Create some objects to help us manage the metadata
				ArrayList<CuePoint> points = new ArrayList<CuePoint>();
				CuePoint point = null;
				Map<String, Object> properties = new HashMap<String, Object>();	

				//Pre roll
				properties.put("url", googleAds[1]);	
				point = new CuePoint(PositionType.BEFORE, cuePointType, properties);
				points.add(point);
				
			    //Mid roll
				properties = new HashMap<String, Object>();
				properties.put("url", googleAds[2]);
				point = new CuePoint(10000, cuePointType, properties);
				points.add(point);
				
				//Post roll
				properties = new HashMap<String, Object>();
				properties.put("url", googleAds[0]);
				point = new CuePoint(PositionType.AFTER, cuePointType, properties);
				points.add(point);
				
				//Create details for the cuepoints
				Map<String, Serializable> details = new HashMap<String, Serializable>();
				details.put(Event.CUE_POINTS, points);
				
				/*
				 * Emit that we are setting cuepoints, this will allow all components to respond
				 * that are interested in listening
				 */
				eventEmitter.emit(EventType.SET_CUE_POINTS, details);
		    }
		});
		
		/*
		 * ADS_REQUEST_FOR_VIDEO is the Brightcove Google IMA plugin event that gets fired when
		 * there is an ads request for a video.  This will allow you to use your cue point data
		 * and respond to the event with ads.  Having this hook allows you to do any additional 
		 * configuration of companion ads or application logic for the ads. 
		 * 
		 */
		videoView.getEventEmitter().on(GoogleIMAEventType.ADS_REQUEST_FOR_VIDEO, new EventListener() {
			
	        public void processEvent(Event event) {
	        	
	            EventEmitter eventEmitter = videoView.getEventEmitter();
	            List<CuePoint> cuePoints = (List<CuePoint>) event.properties.get(Event.CUE_POINTS);
	            ArrayList<SimpleAdsRequest> ads = new ArrayList<SimpleAdsRequest>(cuePoints.size());

	            for(CuePoint point : cuePoints) {
	            	//Create an ad request
	                SimpleAdsRequest adRequest = new SimpleAdsRequest();
	                
	                //Set up a companion slot
	                Collection<CompanionAdSlot> adSlots = new HashSet<CompanionAdSlot>();
	                CompanionAdSlot adSlot = new CompanionAdSlot();
	                
	                //View group and size
	                ViewGroup adCompanionViewGroup = (ViewGroup)findViewById(R.id.ad_frame);
	                adSlot.setContainer(adCompanionViewGroup);
	                adSlot.setSize(728, 90);
	                
	                adSlots.add(adSlot);
	                adRequest.setCompanions(adSlots);
	                
	                //URL and Type
	                adRequest.setAdTagUrl(point.getStringProperty("url"));
	                adRequest.setAdType(AdType.VIDEO);
	                
	                ads.add(adRequest);
	            }  
	            
	            //Respond to the event
	            event.properties.put(GoogleIMAComponent.ADS_REQUESTS, ads);
	            eventEmitter.respond(event);
	        } 
		});
		
		/*
		 * SET_CUE_POINTS is an event signaling that cue points are being set on the video
		 * We will use this as a cue point (pun intended) to play the video
		 */
		videoView.getEventEmitter().on(EventType.SET_CUE_POINTS, new EventListener() {
			
			public void processEvent(Event arg0) {
				//Play on player...
				videoView.getEventEmitter().emit(EventType.PLAY);
			}
		});
		
		videoView.getEventEmitter().on(GoogleIMAEventType.DID_BEGIN_PLAYING_AD, new EventListener() {
			
			@Override
			public void processEvent(Event arg0) {
				
				//Check here to see if you want to hide / show the companion ad
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
}
