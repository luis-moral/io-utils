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
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;

public class FileHelper 
{
	private final static String EXTENSION_PROPERTIES = "properties";
	
	private FileHelper()
	{		
	}
	
	public static URL[] getFiles(URL path) throws IOException
	{
		return getFiles(path, false, EXTENSION_PROPERTIES);
	}
	
	public static URL[] getFiles(URL path, boolean recursive) throws IOException
	{
		return getFiles(path, recursive, EXTENSION_PROPERTIES);
	}
	
	public static URL[] getFiles(URL path, boolean recursive, String...extensions) throws IOException
	{
		URL[] urls = null;
		
		try
		{
			Collection<File> fileCollection = FileUtils.listFiles(new File(path.toURI()), extensions, recursive);
			urls = new URL[fileCollection.size()];
			
			Iterator<File> iterator = fileCollection.iterator();
			int index = 0;
			while (iterator.hasNext())
			{
				File file = iterator.next();
				
				urls[index++] = file.toURI().toURL();
			}
		}
		catch (URISyntaxException USe)
		{
			throw new IOException(USe);
		}
		
		return urls;
	}
}
