/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.projects.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectAssociationProvider;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ProjectListener;
import org.jboss.forge.addon.projects.ProjectLocator;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.MutableAddonRepository;
import org.jboss.forge.furnace.services.ExportedInstance;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.jboss.forge.furnace.util.Predicate;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@Singleton
public class ProjectFactoryImpl implements ProjectFactory
{
   private static final Logger log = Logger.getLogger(ProjectFactoryImpl.class.getName());

   @Inject
   private AddonRegistry registry;

   @Inject
   private ResourceFactory resourceFactory;

   @Inject
   private Furnace forge;

   @Inject
   private FacetFactory factory;

   private final List<ProjectListener> projectListeners = new ArrayList<ProjectListener>();

   @Override
   public Project findProject(FileResource<?> target)
   {
      return findProject(target, null);
   }

   @Override
   public Project findProject(FileResource<?> target, Predicate<Project> filter)
   {
      if (filter == null)
      {
         filter = new Predicate<Project>()
         {
            @Override
            public boolean accept(Project type)
            {
               return true;
            }
         };
      }

      Project result = null;
      for (ExportedInstance<ProjectLocator> instance : registry.getExportedInstances(ProjectLocator.class))
      {
         DirectoryResource r = (target instanceof DirectoryResource) ? (DirectoryResource) target : target.getParent();
         while (r != null && result == null)
         {
            ProjectLocator locator = instance.get();
            if (locator.containsProject(r))
            {
               result = locator.createProject(r);
               if (!filter.accept(result))
                  result = null;
            }

            r = r.getParent();
         }
      }

      if (result != null)
      {
         for (Class<ProjectFacet> instance : registry.getExportedTypes(ProjectFacet.class))
         {
            ProjectFacet facet = factory.create(instance, result);
            if (facet != null && facet.isInstalled() && result.install(facet))
            {
               log.fine("Installed Facet [" + facet + "] into Project [" + result + "]");
            }
         }
      }

      return result;
   }

   @Override
   public Project createProject(DirectoryResource projectDir)
   {
      return createProject(projectDir, null);
   }

   @Override
   public Project createProject(DirectoryResource target, Iterable<Class<? extends ProjectFacet>> facetTypes)
   {
      Project result = null;
      for (ExportedInstance<ProjectLocator> instance : registry.getExportedInstances(ProjectLocator.class))
      {
         ProjectLocator locator = instance.get();
         result = locator.createProject(target);
         if (result != null)
            break;
      }

      if (result != null)
      {
         DirectoryResource parentDir = result.getProjectRoot().getParent().reify(DirectoryResource.class);
         if (parentDir != null)
         {
            for (ExportedInstance<ProjectAssociationProvider> providerInstance : registry
                     .getExportedInstances(ProjectAssociationProvider.class))
            {
               ProjectAssociationProvider provider = providerInstance.get();
               if (provider.canAssociate(result, parentDir))
               {
                  provider.associate(result, parentDir);
               }
            }
         }
      }

      if (result != null && facetTypes != null)
      {
         for (Class<? extends ProjectFacet> facetType : facetTypes)
         {
            ProjectFacet facet = factory.create(facetType, result);
            if (!result.install(facet))
            {
               throw new IllegalStateException("Could not install Facet [" + facet + "] of type [" + facetType
                        + "] into Project [" + result + "]");
            }
         }
      }

      if (result != null)
      {
         for (Class<ProjectFacet> instance : registry.getExportedTypes(ProjectFacet.class))
         {
            ProjectFacet facet = factory.create(instance, result);
            if (facet != null && facet.isInstalled() && result.install(facet))
            {
               log.fine("Installed Facet [" + facet + "] into Project [" + result + "]");
            }
         }
      }

      if (result != null)
         fireProjectCreated(result);

      return result;
   }

   private void fireProjectCreated(Project project)
   {
      for (ProjectListener listener : projectListeners)
      {
         listener.projectCreated(project);
      }
   }

   @Override
   public Project createTempProject()
   {
      List<AddonRepository> repositories = forge.getRepositories();
      File rootDirectory = null;
      for (AddonRepository addonRepository : repositories)
      {
         if (addonRepository instanceof MutableAddonRepository)
         {
            rootDirectory = addonRepository.getRootDirectory();
         }
      }
      if (rootDirectory == null)
      {
         try
         {
            rootDirectory = File.createTempFile("forgeproject", ".tmp");
            rootDirectory.delete();
            rootDirectory.mkdirs();
         }
         catch (IOException e)
         {
            throw new RuntimeException("Could not create temp folder", e);
         }
      }
      DirectoryResource addonDir = resourceFactory.create(DirectoryResource.class, rootDirectory);
      DirectoryResource projectDir = addonDir.createTempResource();
      projectDir.deleteOnExit();
      Project project = createProject(projectDir);
      return project;
   }

   @Override
   public ListenerRegistration<ProjectListener> addProjectListener(final ProjectListener listener)
   {
      projectListeners.add(listener);
      return new ListenerRegistration<ProjectListener>()
      {
         @Override
         public ProjectListener removeListener()
         {
            projectListeners.remove(listener);
            return listener;
         }
      };
   }
}
