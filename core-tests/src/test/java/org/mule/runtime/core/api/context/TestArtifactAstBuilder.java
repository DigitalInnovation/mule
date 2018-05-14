/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import java.util.Set;

import org.mule.runtime.api.artifact.ast.ArtifactAst;
import org.mule.runtime.core.api.artifact.dsl.xml.ArtifactXmlBasedAstBuilder;

public class TestArtifactAstBuilder {

  private ArtifactXmlBasedAstBuilder artifactXmlBasedAstBuilder = ArtifactXmlBasedAstBuilder.builder();

  private TestArtifactAstBuilder() {
    artifactXmlBasedAstBuilder.setClassLoader(Thread.currentThread().getContextClassLoader());
  }

  public static TestArtifactAstBuilder builder() {
    return new TestArtifactAstBuilder();
  }

  public TestArtifactAstBuilder setConfig(Set<String> configFiles) {
    artifactXmlBasedAstBuilder.setConfigFiles(configFiles);
    return this;
  }

  public ArtifactAst build() {
    return artifactXmlBasedAstBuilder.build();
  }

}
