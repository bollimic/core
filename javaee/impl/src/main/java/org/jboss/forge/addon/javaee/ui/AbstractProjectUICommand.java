/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.javaee.ui;

import javax.inject.Inject;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.AbstractUICommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UISelection;

public abstract class AbstractProjectUICommand extends AbstractUICommand
{
   @Inject
   protected ProjectFactory projectFactory;

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
