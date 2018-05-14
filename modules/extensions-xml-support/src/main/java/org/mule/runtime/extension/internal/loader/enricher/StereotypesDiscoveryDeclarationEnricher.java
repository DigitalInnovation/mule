/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.enricher;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.POST_STRUCTURE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.artifact.ast.ComponentAst;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.el.BindingContextUtils;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.config.internal.ArtifactAstHelper;
import org.mule.runtime.config.internal.ComponentAstHolder;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.GlobalElementComponentModelModelProperty;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;


/**
 * {@link DeclarationEnricher} implementation which enriches the {@link ExtensionModel} and their {@link OperationModel} from the
 * used extensions adding the needed {@link StereotypeModel} to the current declaration.
 *
 * @since 4.2
 */
public class StereotypesDiscoveryDeclarationEnricher implements DeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return POST_STRUCTURE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    withContextClassLoader(extensionLoadingContext.getExtensionClassLoader(),
                           () -> new EnricherDelegate(extensionLoadingContext.getDslResolvingContext())
                               .apply(extensionLoadingContext));
  }

  private static class EnricherDelegate {

    final DslResolvingContext dslResolvingContext;

    EnricherDelegate(DslResolvingContext dslResolvingContext) {
      this.dslResolvingContext = dslResolvingContext;
    }

    public void apply(ExtensionLoadingContext extensionLoadingContext) {
      ExtensionDeclaration extensionDeclaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
      Optional<GlobalElementComponentModelModelProperty> modelProperty =
          extensionDeclaration.getModelProperty(GlobalElementComponentModelModelProperty.class);
      new IdempotentDeclarationWalker() {

        @Override
        protected void onConfiguration(ConfigurationDeclaration declaration) {
          walkDeclaration(modelProperty, declaration.getAllParameters());
        }

        @Override
        protected void onConnectionProvider(ConnectedDeclaration owner,
                                            ConnectionProviderDeclaration declaration) {
          if (owner instanceof ConfigurationDeclaration) {
            walkDeclaration(modelProperty, declaration.getAllParameters());
          }
        }
      }.walk(extensionDeclaration);
    }

    private void walkDeclaration(Optional<GlobalElementComponentModelModelProperty> globalElementModelProperty,
                                 List<ParameterDeclaration> allParameters) {
      globalElementModelProperty.ifPresent(modelProperty -> allParameters.stream()
          .filter(parameterDeclaration -> parameterDeclaration.getType() instanceof StringType)
          .forEach(parameterDeclaration -> traverseProperty(globalElementModelProperty.get().getGlobalComponentsAst(),
                                                            parameterDeclaration)));
    }

    /**
     * Given a {@code propertyDeclaration} it will look up all the usages in the global elements of the smart connector where for
     * each occurrence it will extract the stereotypes of the underlying connector's parameter to copy into the current
     * declaration. <br/>
     * If the result of those finding throws repeated stereotypes, then it will reduce that collection by taking the intersection,
     * making its usage within the smart connector compliant.
     *
     * @param globalComponentsAst global elements of the smart connector to read the possible usages of the property.
     * @param parameterDeclaration property used to look up in the global elements' usages, and the one that will hold the
     *        stereotypes if found.
     */
    private void traverseProperty(List<ComponentAst> globalComponentsAst,
                                  ParameterDeclaration parameterDeclaration) {
      final List<List<StereotypeModel>> allowedStereotypeModels = new ArrayList<>();
      globalComponentsAst.forEach(globalComponentAst -> {
        allowedStereotypeModels.add(findStereotypes(new ComponentAstHolder(globalComponentAst), parameterDeclaration));
        ArtifactAstHelper.executeOnNestedProcessors(globalComponentAst, innerComponentAst -> {
          allowedStereotypeModels.add(findStereotypes(innerComponentAst, parameterDeclaration));
        });
      });

      allowedStereotypeModels.stream()
          .filter(stereotypeModels -> !stereotypeModels.isEmpty())
          .reduce((stereotypeModels, stereotypeModels2) -> {
            final List<StereotypeModel> partialIntersection = new ArrayList<>(stereotypeModels);
            partialIntersection.retainAll(stereotypeModels2);
            return partialIntersection;
          }).ifPresent(parameterDeclaration::setAllowedStereotypeModels);

      // final List<List<StereotypeModel>> allowedStereotypeModels = new ArrayList<>();
      // globalElements.forEach(componentModel -> {
      // allowedStereotypeModels.add(findStereotypes(componentModel, propertyDeclaration));
      // componentModel.executedOnEveryInnerComponent(innerComponentModel -> allowedStereotypeModels
      // .add(findStereotypes(innerComponentModel, propertyDeclaration)));
      // });
      //
      // allowedStereotypeModels.stream()
      // .filter(stereotypeModels -> !stereotypeModels.isEmpty())
      // .reduce((stereotypeModels, stereotypeModels2) -> {
      // final List<StereotypeModel> partialIntersection = new ArrayList<>(stereotypeModels);
      // partialIntersection.retainAll(stereotypeModels2);
      // return partialIntersection;
      // }).ifPresent(propertyDeclaration::setAllowedStereotypeModels);
    }

    private List<StereotypeModel> findStereotypes(ComponentAstHolder componentAstHolder,
                                                  ParameterDeclaration propertyDeclaration) {
      final String expectedPropertyReference = ExpressionManager.DEFAULT_EXPRESSION_PREFIX + BindingContextUtils.VARS + "."
          + propertyDeclaration.getName() + ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
      return componentAstHolder.getParameters().stream()
          .filter(parameterAstHolder -> parameterAstHolder.getSimpleParameterValueAst().getRawValue()
              .equals(expectedPropertyReference))
          .map(parameterAst -> parameterAst.getParameterAst().getParameterIdentifier().getIdentifier().getName())
          .map(parameterName -> findStereotypes(componentAstHolder.getComponentAst().getComponentIdentifier(), parameterName))
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    }

    private List<StereotypeModel> findStereotypes(ComponentIdentifier componentModelIdentifier, String attributeName) {
      final List<StereotypeModel> allowedStereotypes = new ArrayList<>();
      dslResolvingContext.getExtensions().stream()
          .filter(extensionModel -> extensionModel.getXmlDslModel().getPrefix().equals(componentModelIdentifier.getNamespace()))
          .findFirst()
          .ifPresent(extensionModel -> {

            final DslSyntaxResolver dslSyntaxResolver = DslSyntaxResolver.getDefault(extensionModel, dslResolvingContext);

            for (ConfigurationModel configurationModel : extensionModel.getConfigurationModels()) {
              allowedStereotypes
                  .addAll(getStereotypeModels(configurationModel, attributeName, dslSyntaxResolver, componentModelIdentifier));
              for (ConnectionProviderModel connectionProviderModel : configurationModel.getConnectionProviders()) {
                allowedStereotypes
                    .addAll(getStereotypeModels(connectionProviderModel, attributeName, dslSyntaxResolver,
                                                componentModelIdentifier));
              }
            }
          });
      return allowedStereotypes;
    }

    private static List<StereotypeModel> getStereotypeModels(ParameterizedModel parameterizedModel, String attributeName,
                                                             DslSyntaxResolver dslSyntaxResolver,
                                                             ComponentIdentifier componentModelIdentifier) {
      List<StereotypeModel> allowedStereotypes = Collections.EMPTY_LIST;
      if (dslSyntaxResolver.resolve(parameterizedModel).getElementName().equals(componentModelIdentifier.getName())) {
        allowedStereotypes = parameterizedModel.getAllParameterModels().stream()
            .filter(parameterModel -> dslSyntaxResolver.resolve(parameterModel).getAttributeName().equals(attributeName))
            .map(ParameterModel::getAllowedStereotypes)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
      }
      return allowedStereotypes;
    }
  }
}
