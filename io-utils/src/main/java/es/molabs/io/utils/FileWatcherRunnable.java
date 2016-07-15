/**
 * Copyright (C) 2016 Luis Moral Guerrero <luis.moral@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.molabs.io.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWatcherRunnable implements Runnable
{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private WatchService watchService = null;
	private Set<URL> fileSet = null;
	private long refreshTime;
	
	private FileWatcherHandler handler = null;
	
	public FileWatcherRunnable(WatchService watchService, FileWatcherHandler handler) throws IOException
	{
		this(watchService, new HashSet<URL>(), 1000, handler);
	}
	
	public FileWatcherRunnable(WatchService watchService, Set<URL> fileSet, long refreshTime, FileWatcherHandler handler) throws IOException
	{
		this.watchService = watchService;
		this.fileSet = fileSet;
		this.refreshTime = refreshTime;
		this.handler = handler;
		
		Iterator<URL> iterator = fileSet.iterator();
		while (iterator.hasNext())
		{
			addToWatchService(iterator.next());
		}
	}
	
	public void addFile(URL file) throws IOException
	{
		fileSet.add(file);
		
		addToWatchService(file);
	}
	
	public void setRefreshTime(long refreshTime)
	{
		this.refreshTime = refreshTime;
	}
	
	public long getRefreshTime()
	{
		return refreshTime;
	}
	
	private void addToWatchService(URL url) throws IOException
	{
		String path = null;
		
		try
		{
			File file = new File(url.toURI());
			
			// If the file is a directory
			if (file.isDirectory())
			{
				path = file.toString();
			}
			// Else
			else
			{
				// Gets the directory that contains the file
				path = new File(url.toURI()).getParent();
			}			
		}
		catch (URISyntaxException USe)
		{
			throw new FileNotFoundException(USe.getLocalizedMessage());
		}
		
		Path pathToWatch = Paths.get(path);
		
		// If the directory exists
		if (pathToWatch == null)
		{
			throw new FileNotFoundException(path);
		}		
				
		pathToWatch.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.OVERFLOW);		
		
		logger.debug("Path Added [{}]", pathToWatch);
	}
	
	public void run() 
	{
		try
		{
			WatchKey key = watchService.take();
			
            while (key != null)
            {
            	// Sleeps the thread
            	Thread.sleep(refreshTime);
            	
            	Iterator<WatchEvent<?>> iterator = key.pollEvents().iterator();
                while (iterator.hasNext())
                {
                	WatchEvent<?> event = iterator.next();
                	
                	Path path = (Path) key.watchable();
            		Path fullPath = path.resolve((Path) event.context());
                	
            		URL urlFile = fullPath.toUri().toURL();
            		
                	// If the modified file is handled by this watcher
            		//if (fileSet.contains(urlFile))
                	{
	                	// If the event is CREATED
	                	if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE)
	                	{
	                		logger.debug("Entry Create [{}].", urlFile);
	                		
	                		handler.entryCreate(urlFile);
	                	}                	
	                	// If the event is MODIFY
	                	else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY)
	                	{                		
	                		logger.debug("Entry Modify [{}].", urlFile);
	                		
	                		handler.entryModify(urlFile);
	                	}
	                	// If the event is DELETE
	                	else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE)
	                	{
	                		logger.debug("Entry Delete [{}].", urlFile);
	                		
	                		handler.entryDelete(urlFile);
	                	}
	                	// If the event is OVERFLOW
	                	else if (event.kind() == StandardWatchEventKinds.OVERFLOW)
	                	{
	                		logger.debug("Entry Overflow [{}].", urlFile);
	                		
	                		handler.overflow(urlFile);
	                	}
                	}
                }
                
                key.reset();
                key = watchService.take();
            }
		}
		catch (ClosedWatchServiceException CWSe)
		{
			// If the service was closed, ignore the error and finish the execution
		}
		catch (Throwable t)
		{
			logger.error(t.getLocalizedMessage(), t);
		}
	}
}
