/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.container;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.forge.container.util.Assert;
import org.jboss.forge.container.util.OSUtils;
import org.jboss.forge.container.util.Streams;
import org.jboss.forge.container.util.Strings;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.parser.xml.XMLParserException;

/**
 * Used to perform Addon installation/registration operations.
 *
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:koen.aers@gmail.com">Koen Aers</a>
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public final class AddonUtil
{
   public static String getRuntimeAPIVersion()
   {
      String version = AddonUtil.class.getPackage()
               .getImplementationVersion();
      return version;
   }

   public static boolean hasRuntimeAPIVersion()
   {
      return getRuntimeAPIVersion() != null;
   }

   public static boolean isApiCompatible(CharSequence runtimeVersion, AddonEntry entry)
   {
      Assert.notNull(runtimeVersion, "Runtime API version must not be null.");
      Assert.notNull(entry, "Addon entry must not be null.");
      String addonApiVersion = entry.getApiVersion();
      Assert.notNull(addonApiVersion, "Addon entry.getApiVersion() must not be null.");

      return isApiCompatible(runtimeVersion, addonApiVersion);
   }

   /**
    * This method only returns true if:
    *
    * - The major version of pluginApiVersion is equal to the major version of runtimeVersion AND
    *
    * - The minor version of pluginApiVersion is less or equal to the minor version of runtimeVersion
    *
    * @param runtimeVersion a version in the format x.x.x
    * @param addonApiVersion a version in the format x.x.x
    * @return
    */
   public static boolean isApiCompatible(CharSequence runtimeVersion, String addonApiVersion)
   {
      Matcher runtimeMatcher = VERSION_PATTERN.matcher(runtimeVersion);
      if (runtimeMatcher.matches())
      {
         int runtimeMajorVersion = Integer.parseInt(runtimeMatcher.group(1));
         int runtimeMinorVersion = Integer.parseInt(runtimeMatcher.group(2));

         Matcher pluginApiMatcher = VERSION_PATTERN.matcher(addonApiVersion);
         if (pluginApiMatcher.matches())
         {
            int pluginApiMajorVersion = Integer.parseInt(pluginApiMatcher.group(1));
            int pluginApiMinorVersion = Integer.parseInt(pluginApiMatcher.group(2));

            if (pluginApiMajorVersion == runtimeMajorVersion && pluginApiMinorVersion <= runtimeMinorVersion)
            {
               return true;
            }
         }
      }
      return false;
   }

   public static final String PROP_ADDON_DIR = "org.jboss.forge.addonDir";
   private static final String DEFAULT_SLOT = "main";
   private static final String ATTR_SLOT = "slot";
   private static final String ATTR_API_VERSION = "api-version";
   private static final String ATTR_NAME = "name";
   private static final String ADDON_DIR_DEFAULT = ".forge/addons";
   private static final String REGISTRY_FILE_NAME = "installed.xml";
   private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(\\.|-)(.*)");

   private File addonDir;

   private AddonUtil(File dir)
   {
      this.addonDir = dir;
   }

   public static AddonUtil forAddonDir(File dir)
   {
      Assert.notNull(dir, "Addon directory must not be null.");
      return new AddonUtil(dir);
   }

   public static AddonUtil forDefaultAddonDir()
   {
      return new AddonUtil(new File(OSUtils.getUserHomePath() + ADDON_DIR_DEFAULT));
   }

   public File getAddonDir()
   {
      if (!addonDir.exists() || !addonDir.isDirectory())
      {
         addonDir.delete();
         System.gc();
         if (!addonDir.mkdirs())
         {
            throw new RuntimeException("Could not create Addon Directory [" + addonDir + "]");
         }
      }
      return addonDir;
   }

   public synchronized File getRegistryFile()
   {
      File registryFile = new File(getAddonDir(), REGISTRY_FILE_NAME);
      try
      {
         if (!registryFile.exists())
         {
            registryFile.createNewFile();

            FileOutputStream out = new FileOutputStream(registryFile);
            try
            {
               Streams.write(XMLParser.toXMLInputStream(XMLParser.parse("<installed></installed>")), out);
            }
            finally
            {
               out.close();
            }
         }
         return registryFile;
      }
      catch (Exception e)
      {
         throw new RuntimeException("Error initializing addon registry file [" + registryFile + "]", e);
      }
   }

   public synchronized List<AddonEntry> listByAPICompatibleVersion(final String version)
   {
      List<AddonEntry> list = listInstalled();
      List<AddonEntry> result = list;

      if (version != null)
      {
         result = new ArrayList<AddonEntry>();
         for (AddonEntry entry : list)
         {
            if (isApiCompatible(version, entry))
            {
               result.add(entry);
            }
         }
      }

      return result;
   }

   public synchronized List<AddonEntry> listInstalled()
   {
      List<AddonEntry> result = new ArrayList<AddonEntry>();
      File registryFile = getRegistryFile();
      try
      {
         Node installed = XMLParser.parse(registryFile);
         if (installed == null)
         {
            return Collections.emptyList();
         }
         List<Node> list = installed.get("addon");
         for (Node addon : list)
         {
            AddonEntry entry = new AddonEntry(addon.getAttribute(ATTR_NAME),
                     addon.getAttribute(ATTR_API_VERSION),
                     addon.getAttribute(ATTR_SLOT));
            result.add(entry);
         }
      }
      catch (XMLParserException e)
      {
         throw new RuntimeException("Invalid syntax in [" + registryFile.getAbsolutePath()
                  + "] - Please delete this file and restart Forge", e);
      }
      catch (FileNotFoundException e)
      {
         // this is OK, no addons installed
      }
      return result;
   }

   public synchronized AddonEntry install(AddonEntry addon)
   {
      return install(addon.getName(), addon.getApiVersion(), addon.getSlot());
   }

   public synchronized AddonEntry install(final String name, final String apiVersion, String slot)
   {
      if (Strings.isNullOrEmpty(name))
      {
         throw new RuntimeException("Addon must not be null");
      }
      if (Strings.isNullOrEmpty(apiVersion))
      {
         throw new RuntimeException("API version must not be null");
      }
      if (Strings.isNullOrEmpty(slot))
      {
         slot = DEFAULT_SLOT;
      }

      List<AddonEntry> installedAddons = listInstalled();
      for (AddonEntry e : installedAddons)
      {
         if (name.equals(e.getName()))
         {
            remove(e);
         }
      }

      File registryFile = getRegistryFile();
      try
      {
         Node installed = XMLParser.parse(registryFile);

         installed.getOrCreate("addon@" + ATTR_NAME + "=" + name + "&" + ATTR_API_VERSION + "=" + apiVersion)
                  .attribute(ATTR_SLOT, slot);
         Streams.write(XMLParser.toXMLInputStream(installed), new FileOutputStream(registryFile));

         return new AddonEntry(name, apiVersion, slot);
      }
      catch (FileNotFoundException e)
      {
         throw new RuntimeException("Could not read [" + registryFile.getAbsolutePath()
                  + "] - ", e);
      }
   }

   public synchronized void remove(final AddonEntry addon)
   {
      if (addon == null)
      {
         throw new RuntimeException("Addon must not be null");
      }

      File registryFile = getRegistryFile();
      if (registryFile.exists())
      {
         try
         {
            Node installed = XMLParser.parse(registryFile);

            Node child = installed.getSingle("addon@" + ATTR_NAME + "=" + addon.getName() + "&"
                     + ATTR_API_VERSION
                     + "=" + addon.getApiVersion());
            installed.removeChild(child);
            Streams.write(XMLParser.toXMLInputStream(installed), new FileOutputStream(registryFile));
         }
         catch (FileNotFoundException e)
         {
            // already removed
         }
      }
   }

   public synchronized AddonEntry get(final AddonEntry addon)
   {
      if (addon == null)
      {
         throw new RuntimeException("Addon must not be null");
      }

      File registryFile = getRegistryFile();
      try
      {
         Node installed = XMLParser.parse(registryFile);

         List<Node> children = installed.get("addon@" + ATTR_NAME + "=" + addon.getName());
         for (Node child : children)
         {
            if (child != null)
            {
               if ((addon.getApiVersion() == null)
                        || addon.getApiVersion().equals(child.getAttribute(ATTR_API_VERSION)))
               {
                  if ((addon.getSlot() == null)
                           || addon.getSlot().equals(child.getAttribute(ATTR_SLOT)))
                  {
                     return new AddonEntry(child.getAttribute(ATTR_NAME),
                              child.getAttribute(ATTR_API_VERSION),
                              child.getAttribute(ATTR_SLOT));
                  }
               }
            }
         }
      }
      catch (FileNotFoundException e)
      {
         // already removed
      }

      return null;
   }

   public synchronized File getAddonResourceDir(AddonEntry found)
   {
      Assert.notNull(found.getSlot(), "Addon slot must be specified.");
      Assert.notNull(found.getName(), "Addon name must be specified.");

      String path = found.getName().replaceAll("\\.", "/");
      File addonDir = new File(getAddonDir(), path + "/" + found.getSlot());
      return addonDir;
   }

   public synchronized File getAddonBaseDir(AddonEntry found)
   {
      Assert.notNull(found.getSlot(), "Addon slot must be specified.");
      Assert.notNull(found.getName(), "Addon name must be specified.");

      String path = found.getName().split("\\.")[0];
      File addonDir = new File(getAddonDir(), path);
      return addonDir;
   }

   public synchronized boolean has(final AddonEntry addon)
   {
      return get(addon) != null;
   }

   public List<File> getAddonResources(AddonEntry found)
   {
      File dir = getAddonResourceDir(found);
      if (dir.exists())
      {
         return Arrays.asList(dir.listFiles(new FilenameFilter()
         {
            @Override
            public boolean accept(File file, String name)
            {
               return name.endsWith(".jar");
            }
         }));
      }
      return new ArrayList<File>();
   }

   public File getAddonSlotDir(AddonEntry addon)
   {
      return new File(getAddonBaseDir(addon).getAbsolutePath() + "/" + addon.getSlot());
   }

   public synchronized List<AddonDependency> getAddonDependencies(AddonEntry addon)
   {
      List<AddonDependency> result = new ArrayList<AddonUtil.AddonDependency>();
      File descriptor = getAddonDescriptor(addon);

      try
      {
         Node installed = XMLParser.parse(descriptor);

         List<Node> children = installed.get("dependency");
         for (Node child : children)
         {
            if (child != null)
            {
               result.add(new AddonDependency(
                        child.getAttribute(ATTR_NAME),
                        child.getAttribute("min-version"),
                        child.getAttribute("max-version"),
                        Boolean.valueOf(child.getAttribute("optional"))));
            }
         }
      }
      catch (FileNotFoundException e)
      {
         // already removed
      }

      return result;
   }

   public synchronized File getAddonDescriptor(AddonEntry addon)
   {
      File descriptorFile = new File(getAddonResourceDir(addon).getAbsolutePath() + "/forge.xml");
      try
      {
         if (!descriptorFile.exists())
         {
            descriptorFile.mkdirs();
            descriptorFile.delete();
            descriptorFile.createNewFile();

            FileOutputStream stream = new FileOutputStream(descriptorFile);
            Streams.write(XMLParser.toXMLInputStream(XMLParser.parse("<addon/>")), stream);
            stream.close();
         }
         return descriptorFile;
      }
      catch (Exception e)
      {
         throw new RuntimeException("Error initializing addon descriptor file.", e);
      }
   }

   public static class AddonDependency
   {
      private String name;
      private String minVersion = null;
      private String maxVersion = null;

      private boolean optional = false;

      public AddonDependency(String name, String minVersion, String maxVersion)
      {
         this(name, minVersion, maxVersion, false);
      }

      public AddonDependency(String name, String minVersion, String maxVersion, boolean optional)
      {
         this.optional = optional;
         this.name = name;
         this.minVersion = minVersion;
         this.maxVersion = maxVersion;
      }

      public String getName()
      {
         return name;
      }

      public String getMinVersion()
      {
         return minVersion;
      }

      public String getMaxVersion()
      {
         return maxVersion;
      }

      public boolean isOptional()
      {
         return optional;
      }

      @Override
      public String toString()
      {
         return "AddonDependency [name=" + name + ", minVersion=" + minVersion + ", maxVersion=" + maxVersion
                  + ", optional=" + optional + "]";
      }

      @Override
      public int hashCode()
      {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((name == null) ? 0 : name.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj)
      {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         AddonDependency other = (AddonDependency) obj;
         if (name == null)
         {
            if (other.name != null)
               return false;
         }
         else if (!name.equals(other.name))
            return false;
         return true;
      }

   }
}