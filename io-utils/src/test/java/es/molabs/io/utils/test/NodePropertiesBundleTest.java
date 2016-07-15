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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import es.molabs.io.utils.FileHelper;
import es.molabs.io.utils.NodePropertiesBundle;
import es.molabs.properties.token.BaseNodePropertiesToken;
import es.molabs.properties.token.ValueTokenLoader;

@RunWith(MockitoJUnitRunner.class)
public class NodePropertiesBundleTest 
{
	@Test
	public void testInitialization() throws Throwable
	{
		NodePropertiesBundle nodePropertiesBundle = new NodePropertiesBundle();
		nodePropertiesBundle.addFile(getClass().getResource("/es/molabs/io/utils/test/bundle/properties/zero.properties"));
		
		// Checks that the properties have been loaded
		testGetProperty(nodePropertiesBundle, "test.property1", null);
		
		// Initializes the manager
		nodePropertiesBundle.init();
		
		// Checks that the properties have been loaded
		testGetProperty(nodePropertiesBundle, "test.property1", "value1");
		
		// Checks that it is initialized
		boolean expectedValue = true;
		boolean value = nodePropertiesBundle.isInitialized();
		Assert.assertEquals("Value must be [" + expectedValue + "].", expectedValue, value);
				
		// Destroys the manager
		nodePropertiesBundle.destroy();
		
		// Checks that the properties have been unloaded
		testGetProperty(nodePropertiesBundle, "test.property1", null);		
		
		// Checks that is not initialized
		expectedValue = false;
		value = nodePropertiesBundle.isInitialized();
		Assert.assertEquals("Value must be [" + expectedValue + "].", expectedValue, value);
		
		// Initializes the manager again
		nodePropertiesBundle.init();
		
		// Checks that the properties have been loaded
		testGetProperty(nodePropertiesBundle, "test.property1", "value1");
		
		// Checks that it is initialized
		expectedValue = true;
		value = nodePropertiesBundle.isInitialized();
		Assert.assertEquals("Value must be [" + expectedValue + "].", expectedValue, value);
		
		nodePropertiesBundle.destroy();
	}
	
	@Test
	public void testNoFiles() throws Throwable
	{
		NodePropertiesBundle nodePropertiesBundle = new NodePropertiesBundle();
		nodePropertiesBundle.init();
		nodePropertiesBundle.destroy();
	}
	
	@Test
	public void testSingleFileNoTokenList() throws Throwable
	{
		NodePropertiesBundle nodePropertiesBundle = new NodePropertiesBundle();
		nodePropertiesBundle.addFile(getClass().getResource("/es/molabs/io/utils/test/bundle/properties/zero.properties"));
		nodePropertiesBundle.init();
		
		testGetProperty(nodePropertiesBundle, "test.property1", "value1");
		
		nodePropertiesBundle.destroy();
	}
	
	@Test
	public void testWeirdPathName() throws Throwable
	{
		NodePropertiesBundle nodePropertiesBundle = new NodePropertiesBundle();
		nodePropertiesBundle.addFile(getClass().getResource("/weird path_name/zero.properties"));
		nodePropertiesBundle.init();
		
		testGetProperty(nodePropertiesBundle, "test.property1", "value1");
		
		nodePropertiesBundle.destroy();
	}
	
	@Test
	public void testSingleFileWithTokenList() throws Throwable
	{
		NodePropertiesBundle nodePropertiesBundle = new NodePropertiesBundle(new BaseNodePropertiesToken(new ValueTokenLoader("STATIC")));		
		nodePropertiesBundle.addFile(getClass().getResource("/es/molabs/io/utils/test/bundle/properties/single.properties"));
		nodePropertiesBundle.init();
		
		testGetProperty(nodePropertiesBundle, "test.property1.value", "STATIC-value");
		
		nodePropertiesBundle.destroy();
	}
	
	@Test
	public void testSinglePath() throws Throwable
	{
		NodePropertiesBundle nodePropertiesBundle = new NodePropertiesBundle();
		nodePropertiesBundle.init();
		
		URL[] files = FileHelper.getFiles(getClass().getResource("/es/molabs/io/utils/test/bundle/properties/"));
		for (int i=0; i<files.length; i++)
		{
			nodePropertiesBundle.addFile(files[i]);
		}
		
		testGetProperty(nodePropertiesBundle, "test.property1", "value1");
		
		nodePropertiesBundle.destroy();
	}
	
	@Test
	public void testPropertiesReload() throws Throwable
	{
		// Make sure that the original value is set (if the test have been launched before)
		Properties properties = new Properties();
		properties.setProperty("*.one.B.property2", "*-one-B-2-value");
		OutputStream output = new FileOutputStream(getClass().getResource("/es/molabs/io/utils/test/bundle/reload/pathone/one-B.properties").getFile());
		properties.store(output, "");
		output.flush();
		output.close();		
		
		NodePropertiesBundle nodePropertiesBundle = new NodePropertiesBundle();
				
		URL[] files = FileHelper.getFiles(getClass().getResource("/es/molabs/io/utils/test/bundle/reload/pathone"));
		for (int i=0; i<files.length; i++)
		{
			nodePropertiesBundle.addFile(files[i]);
		}
		
		files = FileHelper.getFiles(getClass().getResource("/es/molabs/io/utils/test/bundle/reload/pathtwo"));
		for (int i=0; i<files.length; i++)
		{
			nodePropertiesBundle.addFile(files[i]);
		}
		
		nodePropertiesBundle.init();
		
		// Checks get a property
		testGetProperty(nodePropertiesBundle, "*.one.B.property2", "*-one-B-2-value");
		
		// Loads a property file
		InputStream inputStream = getClass().getResourceAsStream("/es/molabs/io/utils/test/bundle/reload/pathone/one-B.properties");		
		properties.load(inputStream);
		inputStream.close();
		
		// Modifies a property
		properties.setProperty("*.one.B.property2", "value-changed");
		output = new FileOutputStream(getClass().getResource("/es/molabs/io/utils/test/bundle/reload/pathone/one-B.properties").getFile());
		properties.store(output, "");
		output.flush();
		output.close();
		
		// Checks that the property loading is thread safe, so a property that exists does not return null because another thread is reloading the values
		long startTime = System.nanoTime();
		
		// Waits for reload
		while (System.nanoTime() < startTime + TimeUnit.MILLISECONDS.toNanos(2000))
		{
			// Checks if the property exists
			Assert.assertNotNull("This property [" + "*.one.B.property2" + "] should exists.", nodePropertiesBundle.getString("*.one.B.property2"));
		}
		
		// Checks that the value has changed
		testGetProperty(nodePropertiesBundle, "*.one.B.property2", "value-changed");		
		
		nodePropertiesBundle.destroy();
	}
	
	private void testGetProperty(NodePropertiesBundle nodePropertiesBundle, String property, String expectedValue)
	{	
		String value = nodePropertiesBundle.getString(property);
		Assert.assertEquals("Value must be [" + expectedValue + "].", expectedValue, value);
	}
}