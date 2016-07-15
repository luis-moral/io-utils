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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.molabs.properties.NodeProperties;
import es.molabs.properties.NodePropertiesKey;
import es.molabs.properties.NodePropertiesToken;

public class NodePropertiesBundle
{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private List<NodePropertiesToken> tokenList = null;
	private NodePropertiesKey nodePropertiesKey = null;
	private NodeProperties nodeProperties = null;
	
	private ExecutorService executorService = null;
	private Map<URL, String> propertiesMap = null;
	private WatchService watchService = null;
	private FileWatcherRunnable fileWatcherRunnable = null;	
	private boolean initialized;
	
	public NodePropertiesBundle(NodePropertiesToken...tokenList)
	{
		this(Arrays.asList(tokenList));
	}
	
	public NodePropertiesBundle(List<NodePropertiesToken> tokenList)
	{
		this.tokenList = tokenList;
		
		nodePropertiesKey = new NodePropertiesKey(tokenList);
		nodeProperties = new NodeProperties(tokenList.size());
				
		propertiesMap = new LinkedHashMap<URL, String>();
		
		initialized = false;
	}
	
	public void init() throws IOException
	{
		if (!initialized)
		{		
			// Sets the bundle as initialized
			initialized = true;
			
			// Starts the watcher
			watchService = FileSystems.getDefault().newWatchService();				
			fileWatcherRunnable = new FileWatcherRunnable(watchService, new ConfigurationFileWatcherHandler());
			
			executorService = Executors.newSingleThreadExecutor(new WatchServiceThreadFactory());
			executorService.submit(fileWatcherRunnable);
			
			// If there is any property to load
			if (!propertiesMap.isEmpty()) loadAll();
			
			logger.info("Initialized.");
		}
		else
		{
			logger.warn("Already initialized.");
		}
	}
	
	public void destroy() throws IOException
	{
		if (initialized)
		{
			// Sets the bundle as not initialized
			initialized = false;
			
			// Stops the watcher
			watchService.close();
			executorService.shutdown();
			
			// Clears the properties
			nodeProperties.clear();			
			
			logger.info("Destroyed.");
		}
		else
		{
			logger.warn("Already destroyed.");
		}
	}
	
	public boolean isInitialized()
	{
		return initialized;
	}
	
	public void addFile(URL resource) throws IOException
	{
		addFile(resource, StandardCharsets.ISO_8859_1.name());
	}
	
	public void addFile(URL resource, String encoding) throws IOException
	{
		// If is already initialized
		if (initialized)
		{
			loadSingle(resource, encoding);
		}
		
		propertiesMap.put(resource, encoding);
	}	
	
	public List<NodePropertiesToken> getTokenList()
	{
		return tokenList;
	}
	
	public String getString(String key)
	{
		return nodeProperties.getProperty(nodePropertiesKey.toKey(key));
	}
	
	private void loadAll() throws IOException
	{		
		// Clears the current values
		nodeProperties.clear();
		
		// For each property managed by this bundle
		Iterator<Map.Entry<URL, String>> iterator = propertiesMap.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<URL, String> entry = iterator.next();			
			
			// Loads it
			loadSingle(entry.getKey(), entry.getValue());
		}		
	}
	
	private void loadSingle(URL resource, String encoding) throws IOException
	{
		loadSingle(resource, encoding, nodeProperties);
	}
	
	private void loadSingle(URL resource, String encoding, NodeProperties container) throws IOException
	{
		Properties propertiesToAdd = new Properties();
		InputStream inputStream = resource.openStream();
		
		try
		{
			propertiesToAdd.load(new BufferedReader(new InputStreamReader(inputStream, encoding)));			
		}
		finally
		{
			inputStream.close();
		}	
		
		container.load(propertiesToAdd);
		
		logger.info("Properties loaded [{}].", resource.getFile());
		
		if (resource.getProtocol().equals("file"))
		{
			fileWatcherRunnable.addFile(resource);
		}
	}
	
	private void reloadAll() throws IOException
	{
		// Creates a new NodeProperties
		NodeProperties newNodeProperties = new NodeProperties(tokenList.size());
		
		// For each properties managed by this bundle
		Iterator<Map.Entry<URL, String>> iterator = propertiesMap.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<URL, String> entry = iterator.next();			
			
			// Adds it to the new NodeProperties
			loadSingle(entry.getKey(), entry.getValue(), newNodeProperties);
		}
		
		// Saves the reference to the old NodeProperties
		NodeProperties oldNodeProperties = nodeProperties;
		
		// Sets the new NodeProperties as the current one
		nodeProperties = newNodeProperties;
		
		// Clears the old values
		oldNodeProperties.clear();
		oldNodeProperties = null;
		newNodeProperties = null;
	}
	
	private class WatchServiceThreadFactory implements ThreadFactory
	{
		private AtomicInteger id = null;
		
		public WatchServiceThreadFactory()
		{
			id = new AtomicInteger(0);
		}
		
		public Thread newThread(Runnable r) 
		{			
			return new Thread(r, "WatchServiceThread-" + id.getAndAdd(1));
		}		
	}
	
	private class ConfigurationFileWatcherHandler implements FileWatcherHandler
	{
		private final Logger logger = LoggerFactory.getLogger(getClass());		

		public void entryCreate(URL file) 
		{			
		}

		public void entryModify(URL file) 
		{
			// If the file modified is managed by this bundle
    		if (propertiesMap.containsKey(file))
    		{
    			logger.debug("File modified: " + file);
        		
    			logger.info("Reloading properties.");
    			
    			// Reloads it
    			try
    			{
    				reloadAll();    				
    			}
    			catch (IOException IOe)
    			{
    				logger.error(IOe.getLocalizedMessage(), IOe);
    			}
        		
        		logger.info("Reloading finished.");
    		}			
		}

		public void entryDelete(URL file) 
		{
		}

		public void overflow(URL file) 
		{
		}		
	}
}
