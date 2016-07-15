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
package es.molabs.io.utils.test;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import es.molabs.io.utils.FileWatcherHandler;
import es.molabs.io.utils.FileWatcherRunnable;

@RunWith(MockitoJUnitRunner.class)
public class FileWatcherRunnableTest 
{
	private final static long REFRESH_MARGIN = 500;
	
	private ExecutorService executor = null;
	
	@Test
	public void testEntryCreate() throws Throwable
	{
		URL file = getClass().getResource("/es/molabs/io/utils/test/filewatcher/");
		
		// Deletes the file if already exists
		File newFile = new File(file.getFile() + File.separator + "test-create.txt");		
		if (newFile.exists()) newFile.delete();		
		
		WatchService watchService = FileSystems.getDefault().newWatchService();
		FileWatcherHandler handler = Mockito.mock(FileWatcherHandler.class);		
		FileWatcherRunnable fileWatcherRunnable = new FileWatcherRunnable(watchService, handler);
		fileWatcherRunnable.addFile(file);
		executor.submit(fileWatcherRunnable);
		
		// Creates the file
		newFile.createNewFile();
		
		// Waits the refresh time
		Thread.sleep(fileWatcherRunnable.getRefreshTime() + REFRESH_MARGIN);
		
		// Checks that the event handler has been called one time		
		Mockito.verify(handler, Mockito.times(1)).entryCreate(Mockito.any());		
		
		// Stops the service
		watchService.close();
	}
	
	@Test
	public void testEntryModify() throws Throwable
	{
		URL file = getClass().getResource("/es/molabs/io/utils/test/filewatcher/test.txt");
		
		WatchService watchService = FileSystems.getDefault().newWatchService();		
		FileWatcherHandler handler = Mockito.mock(FileWatcherHandler.class);
		FileWatcherRunnable fileWatcherRunnable = new FileWatcherRunnable(watchService, handler);
		fileWatcherRunnable.addFile(file);
		executor.submit(fileWatcherRunnable);
		
		// Writes to the file
		FileUtils.write(new File(file.getFile()), "test data.", Charset.defaultCharset());		
		
		// Waits the refresh time
		Thread.sleep(fileWatcherRunnable.getRefreshTime() + REFRESH_MARGIN);
		
		// Checks that the event handler has been called one time		
		Mockito.verify(handler, Mockito.times(1)).entryModify(Mockito.any());		
		
		// Stops the service
		watchService.close();
	}
	
	@Test
	public void testEntryDelete() throws Throwable
	{
		URL file = getClass().getResource("/es/molabs/io/utils/test/filewatcher/");
		
		// Creates the file if it does not exist
		File newFile = new File(file.getFile() + File.separator + "test-delete.txt");		
		if (!newFile.exists()) newFile.createNewFile();		
		
		WatchService watchService = FileSystems.getDefault().newWatchService();
		FileWatcherHandler handler = Mockito.mock(FileWatcherHandler.class);		
		FileWatcherRunnable fileWatcherRunnable = new FileWatcherRunnable(watchService, handler);
		fileWatcherRunnable.addFile(file);
		executor.submit(fileWatcherRunnable);
		
		// Delete the file
		newFile.delete();
		
		// Waits the refresh time
		Thread.sleep(fileWatcherRunnable.getRefreshTime() + REFRESH_MARGIN);
		
		// Checks that the event handler has been called one time		
		Mockito.verify(handler, Mockito.times(1)).entryDelete(Mockito.any());
		
		// Stops the service
		watchService.close();
	}
	
	@Before
	public void setUp()
	{
		executor = Executors.newSingleThreadExecutor();
	}
	
	@After
	public void tearDown()
	{
		executor.shutdown();
	}
}
