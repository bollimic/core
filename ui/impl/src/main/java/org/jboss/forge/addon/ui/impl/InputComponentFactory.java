/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.ui.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.jboss.forge.addon.environment.Environment;
import org.jboss.forge.addon.ui.facets.HintsFacet;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.impl.facets.HintsFacetImpl;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.SelectComponent;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UIInputMany;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.services.Exported;
import org.jboss.forge.furnace.services.ExportedInstance;
import org.jboss.forge.furnace.util.Annotations;

/**
 * Produces UIInput objects
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * 
 */
public class InputComponentFactory
{
   private Environment environment;
   private AddonRegistry addonRegistry;

   @Inject
   public InputComponentFactory(Environment environment, AddonRegistry addonRegistry)
   {
      this.environment = environment;
      this.addonRegistry = addonRegistry;
   }

   @Produces
   @SuppressWarnings("unchecked")
   public <T> UISelectOne<T> produceSelectOne(InjectionPoint injectionPoint)
   {
      String name = injectionPoint.getMember().getName();
      Type type = injectionPoint.getAnnotated().getBaseType();

      if (type instanceof ParameterizedType)
      {
         ParameterizedType parameterizedType = (ParameterizedType) type;

         Type[] typeArguments = parameterizedType.getActualTypeArguments();
         Class<T> valueType = (Class<T>) typeArguments[0];
         WithAttributes withAttributes = injectionPoint.getAnnotated().getAnnotation(WithAttributes.class);
         return createSelectOne(name, valueType, withAttributes);
      }
      else
      {
         throw new IllegalStateException("Cannot inject a generic instance of type " + UISelectOne.class.getName()
                  + "<?,?> without specifying concrete generic types at injection point " + injectionPoint + ".");
      }
   }

   @Produces
   @SuppressWarnings({ "unchecked" })
   public <T> UISelectMany<T> produceSelectMany(InjectionPoint injectionPoint)
   {
      String name = injectionPoint.getMember().getName();
      Type type = injectionPoint.getAnnotated().getBaseType();

      if (type instanceof ParameterizedType)
      {
         ParameterizedType parameterizedType = (ParameterizedType) type;

         Type[] typeArguments = parameterizedType.getActualTypeArguments();
         Class<T> valueType = (Class<T>) typeArguments[0];
         WithAttributes withAttributes = injectionPoint.getAnnotated().getAnnotation(WithAttributes.class);
         return createSelectMany(name, valueType, withAttributes);
      }
      else
      {
         throw new IllegalStateException("Cannot inject a generic instance of type " + UISelectMany.class.getName()
                  + "<?,?> without specifying concrete generic types at injection point " + injectionPoint + ".");
      }
   }

   @Produces
   @SuppressWarnings({ "unchecked" })
   public <T> UIInput<T> produceInput(InjectionPoint injectionPoint)
   {
      String name = injectionPoint.getMember().getName();
      Type type = injectionPoint.getAnnotated().getBaseType();

      if (type instanceof ParameterizedType)
      {
         ParameterizedType parameterizedType = (ParameterizedType) type;

         Type[] typeArguments = parameterizedType.getActualTypeArguments();
         Class<T> valueType = (Class<T>) typeArguments[0];
         WithAttributes withAttributes = injectionPoint.getAnnotated().getAnnotation(WithAttributes.class);
         return createInput(name, valueType, withAttributes);
      }
      else
      {
         throw new IllegalStateException("Cannot inject a generic instance of type " + UIInput.class.getName()
                  + "<?,?> without specifying concrete generic types at injection point " + injectionPoint + ".");
      }
   }

   @Produces
   @SuppressWarnings({ "unchecked" })
   public <T> UIInputMany<T> produceInputMany(InjectionPoint injectionPoint)
   {
      String name = injectionPoint.getMember().getName();
      Type type = injectionPoint.getAnnotated().getBaseType();

      if (type instanceof ParameterizedType)
      {
         ParameterizedType parameterizedType = (ParameterizedType) type;

         Type[] typeArguments = parameterizedType.getActualTypeArguments();
         Class<T> valueType = (Class<T>) typeArguments[0];
         WithAttributes withAttributes = injectionPoint.getAnnotated().getAnnotation(WithAttributes.class);
         return createInputMany(name, valueType, withAttributes);
      }
      else
      {
         throw new IllegalStateException("Cannot inject a generic instance of type " + UIInputMany.class.getName()
                  + "<?,?> without specifying concrete generic types at injection point " + injectionPoint + ".");
      }
   }

   public <T> UIInput<T> createInput(String name, Class<T> valueType, WithAttributes withAttributes)
   {
      UIInputImpl<T> result = new UIInputImpl<T>(name, valueType);
      HintsFacetImpl hintsFacet = new HintsFacetImpl(result, environment);
      result.install(hintsFacet);
      preconfigureInput(result, withAttributes);
      return result;
   }

   public <T> UIInputMany<T> createInputMany(String name, Class<T> valueType, WithAttributes withAttributes)
   {
      UIInputManyImpl<T> result = new UIInputManyImpl<T>(name, valueType);
      HintsFacetImpl hintsFacet = new HintsFacetImpl(result, environment);
      result.install(hintsFacet);
      preconfigureInput(result, withAttributes);
      return result;
   }

   public <T> UISelectOne<T> createSelectOne(String name, Class<T> valueType, WithAttributes withAttributes)
   {
      UISelectOne<T> result = new UISelectOneImpl<T>(name, valueType);
      HintsFacetImpl hintsFacet = new HintsFacetImpl(result, environment);
      result.install(hintsFacet);
      preconfigureInput(result, withAttributes);
      return result;
   }

   public <T> UISelectMany<T> createSelectMany(String name, Class<T> valueType, WithAttributes withAttributes)
   {
      UISelectMany<T> result = new UISelectManyImpl<T>(name, valueType);
      HintsFacetImpl hintsFacet = new HintsFacetImpl(result, environment);
      result.install(hintsFacet);
      preconfigureInput(result, withAttributes);
      return result;
   }

   /**
    * Pre-configure input based on WithAttributes info if annotation exists
    */
   @SuppressWarnings({ "unchecked", "rawtypes" })
   private void preconfigureInput(InputComponent<?, ?> input, WithAttributes atts)
   {
      if (atts != null)
      {
         input.setEnabled(atts.enabled());
         input.setLabel(atts.label());
         input.setRequired(atts.required());
         input.setRequiredMessage(atts.requiredMessage());
         if (atts.type() != InputType.DEFAULT)
         {
            input.getFacet(HintsFacet.class).setInputType(atts.type());
         }
      }

      if (input instanceof SelectComponent)
      {
         SelectComponent selectComponent = (SelectComponent) input;
         Class<?> valueType = input.getValueType();
         Iterable<?> choices = null;
         // Auto-populate Enums on SelectComponents
         if (valueType.isEnum())
         {
            Class<? extends Enum> enumClass = valueType.asSubclass(Enum.class);
            choices = EnumSet.allOf(enumClass);
         }
         // Auto-populate Exported values on SelectComponents
         else if (Annotations.isAnnotationPresent(valueType, Exported.class))
         {
            List<Object> choiceList = new ArrayList<Object>();
            for (ExportedInstance exportedInstance : addonRegistry.getExportedInstances(valueType))
            {
               choiceList.add(exportedInstance.get());
            }
            choices = choiceList;
         }
         selectComponent.setValueChoices(choices);
      }
   }

}
