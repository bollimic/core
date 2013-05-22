package org.example;

import javax.inject.Inject;

import org.example.other.OtherExampleAddon;
import org.jboss.forge.furnace.services.Exported;

@Exported
public class Example3Service
{
   @Inject
   private OtherExampleAddon service;

   public int getRemoteHashCode()
   {
      return service.hashCode();
   }
}
