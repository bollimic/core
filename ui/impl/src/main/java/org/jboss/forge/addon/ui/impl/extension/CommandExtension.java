/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.ui.impl.extension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.jboss.forge.addon.ui.Command;
import org.jboss.forge.addon.ui.UICommand;
import org.jboss.forge.addon.ui.impl.InputComponentFactory;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.furnace.services.Exported;
import org.jboss.forge.furnace.util.Annotations;
import org.jboss.forge.furnace.util.BeanManagerUtils;
import org.jboss.forge.furnace.util.cdi.BeanBuilder;
import org.jboss.forge.furnace.util.cdi.ContextualLifecycle;

public class CommandExtension implements Extension
{
   private Set<AnnotatedMethod<?>> annotationMethods = new HashSet<AnnotatedMethod<?>>();

   /**
    * {@link AnnotationAdapterUICommand} should be vetoed manually since the container ignores it when {@link Vetoed} is
    * used directly
    */
   void vetoAnnotationAdapter(@Observes ProcessAnnotatedType<AnnotationAdapterUICommand> bean)
   {
      bean.veto();
   }

   public <T> void observeAnnotationMethods(@Observes ProcessAnnotatedType<T> bean)
   {
      AnnotatedType<T> annotatedType = bean.getAnnotatedType();
      for (AnnotatedMethod<? super T> annotatedMethod : annotatedType.getMethods())
      {
         if (annotatedMethod.isAnnotationPresent(Command.class))
         {
            annotationMethods.add(annotatedMethod);
         }
      }
   }

   public void registerNewBeans(@Observes final AfterBeanDiscovery discovery, final BeanManager manager)
   {
      for (final AnnotatedMethod<?> method : annotationMethods)
      {
         Bean<?> bean = new BeanBuilder<AnnotationAdapterUICommand>(manager)
                  .beanClass(AnnotationAdapterUICommand.class)
                  .types(AnnotationAdapterUICommand.class, UICommand.class)
                  .beanLifecycle(new ContextualLifecycle<AnnotationAdapterUICommand>()
                  {
                     @Override
                     public AnnotationAdapterUICommand create(Bean<AnnotationAdapterUICommand> bean,
                              CreationalContext<AnnotationAdapterUICommand> creationalContext)
                     {
                        InputComponentFactory factory = BeanManagerUtils.getContextualInstance(manager,
                                 InputComponentFactory.class);
                        Object instance = BeanManagerUtils.getContextualInstance(manager, method.getDeclaringType()
                                 .getJavaClass());
                        List<InputComponent<?, ?>> inputs = new ArrayList<InputComponent<?, ?>>();
                        for (AnnotatedParameter<?> parameter : method.getParameters())
                        {
                           WithAttributes withAttributes = parameter.getAnnotation(WithAttributes.class);
                           // TODO: since Java won't store param names, fetch from somewhere (new annotation?)
                           String paramName = "param" + parameter.getPosition();
                           Class<?> paramType = (Class<?>) parameter.getBaseType();
                           InputComponent<?, ?> input;
                           if (paramType.isEnum() || Annotations.isAnnotationPresent(paramType, Exported.class))
                           {
                              input = factory.createSelectOne(paramName, paramType, withAttributes);
                           }
                           else
                           {
                              input = factory.createInput(paramName, paramType, withAttributes);
                           }
                           inputs.add(input);
                        }
                        return new AnnotationAdapterUICommand(method.getJavaMember(), instance, inputs);
                     }

                     @Override
                     public void destroy(Bean<AnnotationAdapterUICommand> bean, AnnotationAdapterUICommand instance,
                              CreationalContext<AnnotationAdapterUICommand> creationalContext)
                     {
                        creationalContext.release();
                     }
                  }).
                  create();
         discovery.addBean(bean);
      }
   }
}
