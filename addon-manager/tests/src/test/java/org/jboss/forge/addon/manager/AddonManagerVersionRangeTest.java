/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.manager;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.dependencies.DependencyMetadata;
import org.jboss.forge.addon.dependencies.DependencyQuery;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.arquillian.Addon;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * FIXME This test needs to be refactored to be a bit less brittle. It breaks when addon POMs change.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class AddonManagerVersionRangeTest
{
   @Deployment
   @Dependencies({
            @Addon(name = "org.jboss.forge.addon:addon-manager", version = "2.0.0-SNAPSHOT"),
            @Addon(name = "org.jboss.forge.addon:maven", version = "2.0.0-SNAPSHOT")
   })
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap
               .create(ForgeArchive.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.addon:addon-manager",
                                 "2.0.0-SNAPSHOT")
               );

      return archive;
   }

   @Inject
   private AddonRegistry registry;

   @Inject
   private AddonManager addonManager;

   @Inject
   private AddonRepository repository;

   @Inject
   private DependencyResolver resolver;

   @Test
   public void testResolveMetadata() throws Exception
   {
      DependencyQuery query = DependencyQueryBuilder
               .create("org.jboss.forge.addon:example3:jar:forge-addon:2.0.0-SNAPSHOT");
      DependencyMetadata metadata = resolver.resolveDependencyMetadata(query);
      Assert.assertNotNull(metadata);
      Assert.assertTrue(metadata.getDependencies().contains(
               DependencyBuilder.create("junit:junit:4.11")));
   }

   @Test
   public void testInstallingAddonWithSingleOptionalAddonDependency() throws InterruptedException
   {
      int addonCount = registry.getAddons().size();
      AddonId example3 = AddonId.fromCoordinates("org.jboss.forge.addon:example3,2.0.0-SNAPSHOT");
      InstallRequest request = addonManager.install(example3);

      Assert.assertEquals(0, request.getRequiredAddons().size());
      Assert.assertEquals(2, request.getOptionalAddons().size());

      request.perform();

      Assert.assertTrue(repository.isEnabled(example3));
      Assert.assertEquals(1, repository.getAddonResources(example3).size());
      Assert.assertTrue(repository.getAddonResources(example3).contains(
               new File(repository.getAddonBaseDir(example3), "example3-2.0.0-SNAPSHOT-forge-addon.jar")));

      Set<AddonDependencyEntry> dependencies = repository.getAddonDependencies(example3);
      Assert.assertEquals(2, dependencies.size());

      Addons.waitUntilStarted(registry.getAddon(example3), 10, TimeUnit.SECONDS);
      Assert.assertEquals(addonCount + 3, registry.getAddons().size());
   }

}
