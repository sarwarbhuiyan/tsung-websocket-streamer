package com.xively.hackathon.websocket.streams.tsung;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * Simple class to implement a file watcher using the java nio FileWatcher
 * service and listen for changes
 * 
 */
public class FileWatcher extends Thread {

	private final WatchService watchService;
	private final String filePath;
	private final WatchKey watchKey;
	private final BufferedReader reader;
	private final Queue<LogEvent> queue;
	private final List<FileWatchListener> listeners;

	public FileWatcher(String dir, String filePath) throws IOException {
		this.queue = new LinkedBlockingQueue<LogEvent>();
		this.listeners = new LinkedList<FileWatchListener>();
		this.filePath = filePath;
		reader = new BufferedReader(new FileReader(dir + "/" + filePath));
		readFileIntoMemory();
		Path path = FileSystems.getDefault().getPath(dir);
		watchService = FileSystems.getDefault().newWatchService();
		this.watchKey = path.register(watchService,
				StandardWatchEventKinds.ENTRY_MODIFY);
	}

	private void readFileIntoMemory() throws IOException {
		System.out.println("My file has changed");
		String line = reader.readLine();
		// skip to line beginning after #

		while (line == null || line.startsWith("#"))
			line = reader.readLine();

		StringBuilder stringBuilder = new StringBuilder();
		// read to end of file
		while (line != null) {
			System.out.println(line);
			if (!line.startsWith("#")) {
				stringBuilder.append(line + "\n");
			} else {

				LogEvent logEvent = new LogEvent(stringBuilder.toString());
				queue.add(logEvent);

				fireWatchListeners(logEvent);

				stringBuilder = new StringBuilder();

			}
			line = reader.readLine();
		}
		LogEvent logEvent = new LogEvent(stringBuilder.toString());
		queue.add(logEvent);
		fireWatchListeners(logEvent);
	}

	public void addFileWatchListener(FileWatchListener fileWatchListener) {
		this.listeners.add(fileWatchListener);
		for (LogEvent event : queue) {
			fireWatchListeners(event);
		}
	}

	public void removeFileWatchListener(FileWatchListener fileWatchListener) {
		this.listeners.remove(fileWatchListener);
	}
	
	public void fireWatchListeners(final LogEvent logEvent) {
		new Thread(){
			public void run() {
				for (FileWatchListener listener : listeners) {
					listener.stateChanged(logEvent);
				}
			};
		}.start();
	}

	@Override
	public void run() {
		while (true) {
			WatchKey wk;
			try {
				wk = watchService.take();
				for (WatchEvent<?> event : wk.pollEvents()) {
					// we only register "ENTRY_MODIFY" so the context is always
					// a Path.
					final Path changed = (Path) event.context();
					System.out.println(changed);
					if (changed.endsWith(this.filePath)) {
						readFileIntoMemory();
					}
				}
				// reset the key
				boolean valid = wk.reset();
				if (!valid) {
					System.out.println("Key has been unregistered");
				}

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

//	public static void main(String[] args) {
//		try {
//			FileWatcher fileWatcher = new FileWatcher(".", "tsung.log");
//			FileWatchListener fileWatchListener = new FileWatchListener() {
//
//				@Override
//				public void stateChanged(LogEvent logEvent) {
//					System.out
//							.println("Woo file changed" + logEvent.toString());
//				}
//			};
//
//			Thread thread = new Thread(fileWatcher);
//			thread.start();
//			fileWatcher.addFileWatchListener(fileWatchListener);
//
//			thread.join();
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}
}
