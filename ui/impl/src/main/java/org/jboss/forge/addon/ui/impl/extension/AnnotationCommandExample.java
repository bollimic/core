/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.ui.impl.extension;

import java.lang.annotation.ElementType;

import org.jboss.forge.addon.ui.Command;
import org.jboss.forge.addon.ui.metadata.WithAttributes;

public class AnnotationCommandExample
{
   @Command("field")
   public String executeField(@WithAttributes(label = "Field Name:") String name,
            @WithAttributes(label = "Element Type:") ElementType elementType,
            String anyData)
   {
      System.out.println("EXECUTED FIELD with name =" + name + " and elementType=" + elementType);
      return "Hello there !";
   }
}
