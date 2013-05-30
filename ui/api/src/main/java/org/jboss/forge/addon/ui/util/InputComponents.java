/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.jboss.forge.addon.convert.CompositeConverter;
import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.facets.Facet;
import org.jboss.forge.addon.ui.facets.HintsFacet;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.ManyValued;
import org.jboss.forge.addon.ui.input.SelectComponent;
import org.jboss.forge.addon.ui.input.SingleValued;
import org.jboss.forge.furnace.util.Strings;

/**
 * Utilities for {@link InputComponent} objects
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class InputComponents
{
   private static final String COLON = ":";

   /**
    * @return the {@link InputType} object associated to this {@link InputComponent}
    */
   public static InputType getInputType(InputComponent<?, ?> input)
   {
      InputType result = InputType.DEFAULT;
      for (Facet f : input.getFacets())
      {
         if (HintsFacet.class.isInstance(f))
         {
            result = ((HintsFacet) f).getInputType();
            break;
         }
      }
      // FIXME: The following code does NOT work when called from Eclipse. Could it be a bug in CLAC ?
      // if (input.hasFacet(HintsFacet.class))
      // {
      // HintsFacet facet = input.getFacet(HintsFacet.class);
      // result = facet.getInputType();
      // }
      return result;
   }

   /**
    * @return the value stored in this {@link InputComponent}
    */
   public static Object getValueFor(InputComponent<?, ?> component)
   {
      if (component instanceof SingleValued)
      {
         return ((SingleValued<?, Object>) component).getValue();
      }
      else if (component instanceof ManyValued)
      {
         return ((ManyValued<?, Object>) component).getValue();
      }
      else
      {
         return null;
      }
   }

   /**
    * Sets the value in the provided {@link InputComponent}, making any necessary conversions
    * 
    * @param component
    * @param value
    */
   public static void setValueFor(final ConverterFactory converterFactory, final InputComponent<?, Object> component,
            final Object value)
   {
      if (component instanceof SingleValued)
      {
         setSingleInputValue(converterFactory, component, value);
      }
      else if (component instanceof ManyValued)
      {
         setManyInputValue(converterFactory, component, value);
      }
   }

   private static void setSingleInputValue(final ConverterFactory converterFactory,
            final InputComponent<?, Object> input, final Object value)
   {
      final Object convertedType;
      if (value != null)
      {
         convertedType = convertToUIInputValue(converterFactory, input, value);
      }
      else
      {
         convertedType = null;
      }
      ((SingleValued) input).setValue(convertedType);
   }

   private static void setManyInputValue(final ConverterFactory converterFactory,
            final InputComponent<?, Object> input, Object value)
   {
      final Iterable<Object> convertedValues;
      if (value != null)
      {
         List<Object> convertedValuesList = new ArrayList<Object>();
         if (value instanceof Iterable)
         {
            for (Object itValue : (Iterable) value)
            {
               convertedValuesList.add(convertToUIInputValue(converterFactory, input, itValue));
            }
         }
         else
         {
            convertedValuesList.add(convertToUIInputValue(converterFactory, input, value));
         }
         convertedValues = convertedValuesList;
      }
      else
      {
         convertedValues = null;
      }
      ((ManyValued) input).setValue(convertedValues);
   }

   /**
    * Returns the converted value that matches the input
    * 
    * @param input
    * @param value
    * @return
    */
   private static Object convertToUIInputValue(final ConverterFactory converterFactory,
            final InputComponent<?, Object> input, final Object value)
   {
      Object convertedType = value;
      Class<Object> sourceType = (Class<Object>) value.getClass();
      Class<Object> targetType = input.getValueType();
      Converter<String, Object> valueConverter = input.getValueConverter();
      if (!targetType.isAssignableFrom(sourceType))
      {
         if (valueConverter != null)
         {
            if (value instanceof String)
            {
               convertedType = valueConverter.convert((String) value);
            }
            else
            {
               Converter<Object, String> stringConverter = converterFactory.getConverter(sourceType, String.class);
               CompositeConverter compositeConverter = new CompositeConverter(stringConverter, valueConverter);
               convertedType = compositeConverter.convert(value);
            }

         }
         else
         {
            Converter<Object, Object> converter = converterFactory.getConverter(sourceType, targetType);
            convertedType = converter.convert(value);
         }
      }
      return convertedType;
   }

   /**
    * Returns if there is a value set for this {@link InputComponent}
    */
   public static boolean hasValue(InputComponent<?, ?> input)
   {
      boolean ret;
      Object value = InputComponents.getValueFor(input);
      if (value == null)
      {
         ret = false;
      }
      else if (value instanceof String && value.toString().isEmpty())
      {
         ret = false;
      }
      else
      {
         ret = true;
      }
      return ret;
   }

   /**
    * Validate if the input has a value. If not, return the error message
    * 
    * @param input
    * @return
    */
   public static String validateRequired(final InputComponent<?, ?> input)
   {
      String requiredMessage = null;
      if (input.isRequired() && !InputComponents.hasValue(input))
      {
         requiredMessage = input.getRequiredMessage();
         if (Strings.isNullOrEmpty(requiredMessage))
         {
            String labelValue = input.getLabel() == null ? input.getName() : input.getLabel();
            // Chop : off
            if (labelValue.endsWith(COLON))
            {
               labelValue = labelValue.substring(0, labelValue.length() - 1);
            }
            requiredMessage = labelValue + " must be specified.";
         }
      }
      return requiredMessage;
   }

   /**
    * 
    * Returns the item label converter, that is
    * 
    * @param converterFactory May be null
    * @param input
    * @return the item label converter of a {@link SelectComponent} or a {@link Converter} instance from the
    *         {@link ConverterFactory} parameter if not null
    */
   public static Converter<?, String> getItemLabelConverter(final ConverterFactory converterFactory,
            final SelectComponent<?, ?> input)
   {
      Converter<?, String> converter = input.getItemLabelConverter();
      if (converter == null && converterFactory != null)
      {
         converter = converterFactory.getConverter(input.getValueType(), String.class);
      }
      return converter;
   }

   /**
    * Returns the label for this component
    * 
    * @param input the input component
    * @param addColon should a colon be added in the end of the label ?
    * @return the label with a colon in the end if addColon is true
    */
   public static String getLabelFor(InputComponent<?, ?> input, boolean addColon)
   {
      String label = input.getLabel();
      // If no label was provided, use name
      if (label == null)
      {
         label = input.getName();
      }
      // if a colon is required, add it
      if (addColon && !label.endsWith(COLON))
      {
         label += COLON;
      }
      return label;
   }
}
