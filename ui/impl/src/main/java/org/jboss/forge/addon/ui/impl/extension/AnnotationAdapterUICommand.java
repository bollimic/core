/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.ui.impl.extension;

import java.lang.reflect.Method;
import java.util.List;

import org.jboss.forge.addon.ui.UICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.InputComponents;
import org.jboss.forge.addon.ui.util.Metadata;

/**
 * This class acts as an adapter to the UI API for methods with the annotation @Command
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * 
 */
public class AnnotationAdapterUICommand implements UICommand
{
   private List<InputComponent<?, ?>> inputs;
   private Method method;
   private Object instance;

   public AnnotationAdapterUICommand(Method method, Object instance, List<InputComponent<?, ?>> inputs)
   {
      super();
      this.method = method;
      this.instance = instance;
      this.inputs = inputs;
   }

   @Override
   public UICommandMetadata getMetadata()
   {
      return Metadata.forCommand(getClass()).name(method.getName());
   }

   @Override
   public boolean isEnabled(UIContext context)
   {
      return true;
   }

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      for (InputComponent<?, ?> input : inputs)
      {
         builder.add(input);
      }
   }

   @Override
   public void validate(UIValidationContext validator)
   {
      // TODO
   }

   @Override
   public Result execute(UIContext context) throws Exception
   {
      int size = inputs.size();
      Object[] args = new Object[size];
      for (int i = 0; i < size; i++)
      {
         args[i] = InputComponents.getValueFor(inputs.get(i));
      }
      Object result = method.invoke(instance, args);
      if (result == null)
      {
         return Results.success();
      }
      else if (result instanceof Result)
      {
         return (Result) result;
      }
      else
      {
         return Results.success(result.toString());
      }
   }
}
