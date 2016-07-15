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

import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import es.molabs.io.utils.FileHelper;

@RunWith(MockitoJUnitRunner.class)
public class FileHelperTest 
{
	@Test
	public void testGetFiles() throws Throwable
	{
		URL[] files = FileHelper.getFiles(getClass().getResource("/es/molabs/io/utils/filehelper/files/"));
		
		int expectedValue = 2;
		int value = files.length;
		Assert.assertEquals("Value must be [" + expectedValue + "].", expectedValue, value);
	}
}
