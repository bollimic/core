/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.repositories;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.SingleVersionRange;
import org.jboss.forge.furnace.versions.Version;
import org.jboss.forge.furnace.versions.VersionRange;

/**
 * Represents an {@link Addon} dependency as specified in its originating {@link AddonRepository}.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AddonDependencyEntry
{
   private String name;
   private Version apiVersion;
   private VersionRange version;
   private boolean exported;
   private boolean optional;

   /**
    * Return <code>true</code> if this dependency is optional.
    */
   public boolean isOptional()
   {
      return optional;
   }

   /**
    * Return <code>true</code> if this dependency is exported.
    */
   public boolean isExported()
   {
      return exported;
   }

   /**
    * Get the dependency name.
    */
   public String getName()
   {
      return name;
   }

   /**
    * Get the dependency {@link VersionRange}.
    */
   public VersionRange getVersion()
   {
      return version;
   }

   /**
    * Get the required API version.
    * <p>
    * TODO This should probably be a {@link VersionRange}
    */
   public Version getApiVersion()
   {
      return apiVersion;
   }

   /**
    * Create a new {@link AddonDependencyEntry} with the given attributes.
    */
   public static AddonDependencyEntry create(String name, String version)
   {
      return create(name, new SingleVersionRange(new SingleVersion(version)), null, false, false);
   }

   /**
    * Create a new {@link AddonDependencyEntry} with the given attributes.
    */
   public static AddonDependencyEntry create(String name, String version, boolean exported,
            boolean optional)
   {
      return create(name, new SingleVersionRange(new SingleVersion(version)), null, exported, optional);
   }

   /**
    * Create a new {@link AddonDependencyEntry} with the given attributes.
    */
   public static AddonDependencyEntry create(String name, VersionRange version)
   {
      return create(name, version, null, false, false);
   }

   /**
    * Create a new {@link AddonDependencyEntry} with the given attributes.
    */
   public static AddonDependencyEntry create(String name, VersionRange version, Version apiVersion)
   {
      return create(name, version, apiVersion, false, false);
   }

   /**
    * Create a new {@link AddonDependencyEntry} with the given attributes.
    */
   public static AddonDependencyEntry create(String name, VersionRange version, boolean exported,
            boolean optional)
   {
      return create(name, version, null, exported, optional);
   }

   /**
    * Create a new {@link AddonDependencyEntry} with the given attributes.
    */
   public static AddonDependencyEntry create(String name, VersionRange version, Version apiVersion, boolean exported,
            boolean optional)
   {
      Assert.notNull(name, "Addon name must not be null.");
      Assert.notNull(version, "Addon version must not be null.");

      AddonDependencyEntry entry = new AddonDependencyEntry();
      entry.name = name;
      entry.version = version;
      entry.apiVersion = apiVersion;
      entry.exported = exported;
      entry.optional = optional;
      return entry;
   }

   @Override
   public String toString()
   {
      return "name=" + name + ", version=" + version + ", exported=" + exported + ", optional="
               + optional + ", apiVersion=" + apiVersion;
   }

}
