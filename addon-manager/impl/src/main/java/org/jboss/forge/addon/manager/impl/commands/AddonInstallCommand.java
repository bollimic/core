package org.jboss.forge.addon.manager.impl.commands;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.forge.addon.manager.AddonManager;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.UICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UISelection;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.furnace.addons.AddonId;

@Singleton
public class AddonInstallCommand implements UICommand
{

   @Inject
   private AddonManager addonManager;

   @Inject
   @WithAttributes(label = "Group ID", required = true)
   private UIInput<String> groupId;

   @Inject
   @WithAttributes(label = "Name", required = true)
   private UIInput<String> name;

   @Inject
   @WithAttributes(label = "Version", required = true)
   private UIInput<String> version;

   @Inject
   private ProjectFactory projectFactory;

   @Override
   public UICommandMetadata getMetadata()
   {
      return Metadata.forCommand(getClass()).name(AddonCommandConstants.ADDON_INSTALL_COMMAND_NAME)
               .description(AddonCommandConstants.ADDON_INSTALL_COMMAND_DESCRIPTION);
   }

   @Override
   public boolean isEnabled(UIContext context)
   {
      return true;
   }

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      Project project = getSelectedProject(builder.getUIContext());
      if (project != null)
      {
         MetadataFacet facet = project.getFacet(MetadataFacet.class);
         groupId.setDefaultValue(facet.getTopLevelPackage());
         name.setDefaultValue(facet.getProjectName());
         version.setDefaultValue(facet.getProjectVersion());
      }
      builder.add(groupId).add(name).add(version);
   }

   @Override
   public void validate(UIValidationContext context)
   {
   }

   @Override
   public Result execute(UIContext context)
   {
      String coordinates = getCoordinates();
      try
      {
         addonManager.install(AddonId.fromCoordinates(coordinates)).perform();
         return Results.success("Addon " + coordinates + " was installed succesfully.");
      }
      catch (Throwable t)
      {
         return Results.fail("Addon " + coordinates + " could not be installed.", t);
      }
   }

   protected String getCoordinates()
   {
      return groupId.getValue() + ':' + name.getValue() + ',' + version.getValue();
   }

   /**
    * Returns the selected project. null if no project is found
    */
   protected Project getSelectedProject(UIContext context)
   {
      Project project = null;
      UISelection<FileResource<?>> initialSelection = context.getInitialSelection();
      if (initialSelection != null)
      {
         project = projectFactory.findProject(initialSelection.get());
      }
      return project;
   }
}
