/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.projects.impl;

import java.lang.annotation.ElementType;

import org.jboss.forge.addon.ui.Command;
import org.jboss.forge.addon.ui.metadata.WithAttributes;

public class MyCommand
{

   @Command("command")
   public String fromAnotherAddon(@WithAttributes(label = "Another Addon Field Name:") String name,
            @WithAttributes(label = "Element Type:") ElementType elementType,
            Gender gender,
            String anyData)
   {
      System.out.println("EXECUTED FROM A NEW ADDON with name =" + name + " and elementType=" + elementType
               + " - and gender " + gender
               );
      return "Hello there !";
   }

}
