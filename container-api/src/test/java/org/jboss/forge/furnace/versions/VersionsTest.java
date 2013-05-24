/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.versions;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class VersionsTest
{

   @Test
   public void testAreEqual()
   {
      Assert.assertEquals(new SingleVersion("1"), new SingleVersion("1"));
      Assert.assertEquals(new SingleVersion("1.1"), new SingleVersion("1.1"));
      Assert.assertEquals(new SingleVersion("1.1.1"), new SingleVersion("1.1.1"));
      Assert.assertEquals(new SingleVersion("1.1.1-SNAPSHOT"), new SingleVersion("1.1.1-SNAPSHOT"));
      Assert.assertNotEquals(new SingleVersion("1"), new SingleVersion("2"));
      Assert.assertNotEquals(new SingleVersion("1.1"), new SingleVersion("1.1.1"));
      Assert.assertNotEquals(new SingleVersion("1.1.1-SNAPSHOT"), new SingleVersion("1.1.1"));
   }

}
