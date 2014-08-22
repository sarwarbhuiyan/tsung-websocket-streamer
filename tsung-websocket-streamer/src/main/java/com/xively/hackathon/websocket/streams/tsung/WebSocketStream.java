package com.xively.hackathon.websocket.streams.tsung;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.atmosphere.config.service.WebSocketHandlerService;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.util.SimpleBroadcaster;
import org.atmosphere.websocket.WebSocket;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
import org.atmosphere.websocket.WebSocketHandler;
import org.atmosphere.websocket.WebSocketStreamingHandlerAdapter;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple {@link WebSocketHandler} that implement the logic to build a streaming application emitting lines from a Tsung file
 *
 * @author Sarwar Bhuiyan
 */
@WebSocketHandlerService(path = "/stream", broadcaster = SimpleBroadcaster.class,
        atmosphereConfig = {"org.atmosphere.websocket.WebSocketProtocol=org.atmosphere.websocket.protocol.StreamingHttpProtocol"})
public class WebSocketStream extends WebSocketStreamingHandlerAdapter {
	
	private static FileWatcher fileWatcher;
	
    private final Logger logger = LoggerFactory.getLogger(WebSocketStream.class);

    static {
    	try {
			fileWatcher = new FileWatcher(".", "tsung.log");
			fileWatcher.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    // A thread which sends a stream of data out of a websocket. Create when the class
    // is instantiated, inject the websocket when open.
    private class Stream implements FileWatchListener {
    	protected WebSocket socket;
    	protected final ObjectMapper mapper = new ObjectMapper();
    	protected boolean stop = false;

    	public Stream(WebSocket socket) {
    		this.socket = socket;
    	}

    	@Override
		public void stateChanged(LogEvent logEvent) {
    		if(socket==null || !socket.isOpen())
    			fileWatcher.removeFileWatchListener(this);
    		
			Map<String, Object> message = new HashMap<String, Object>();
			
			String string;
			try {
				string = mapper.writeValueAsString(logEvent.getPayload());
				
				socket.write(string);
			} catch (JsonGenerationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
		}
    }

    int clients = 0;

    @Override
    public void onOpen(WebSocket webSocket) throws IOException {
    	// Hook up the stream.
    	final Stream stream = new Stream(webSocket);
    	if(fileWatcher!=null)
    		fileWatcher.addFileWatchListener(stream);

    	webSocket.broadcast("client " + clients++ + " connected");
        webSocket.resource().addEventListener(new WebSocketEventListenerAdapter() {
            @Override
            public void onDisconnect(AtmosphereResourceEvent event) {
                if (event.isCancelled()) {
                    logger.info("Browser {} unexpectedly disconnected", event.getResource().uuid());
                } else if (event.isClosedByClient()) {
                    logger.info("Browser {} closed the connection", event.getResource().uuid());
                }
                stream.stop = true;
            }
        });
    }
}
