/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.test.petstore.extension.PetStoreClient;
import org.mule.test.petstore.extension.PetStoreConnectionProvider;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.petstore.extension.PetStoreOperations;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PetStoreRetryPolicyProviderConnectionTestCase extends ExtensionFunctionalTestCase {

  public static final String CONNECTION_FAIL = "Connection fail";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  public PetStoreRetryPolicyProviderConnectionTestCase() {}

  @Override
  protected String getConfigFile() {
    return "petstore-retry-policy.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {PetStoreConnectorWithConnectionFailure.class};
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionExecutingOperation() throws Exception {
    exception.expect(MessagingException.class);
    exception.expectCause(is(instanceOf(ConnectionException.class)));
    runFlow("fail-operation-with-connection-exception");
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionAtValidateTime() throws Exception {
    exception.expect(MessagingException.class);
    exception.expectCause(is(instanceOf(ConnectionException.class)));
    runFlow("fail-connection-validation");
  }

  @Test
  public void retryPolicyNotExecutedDueToNotConnectionExceptionWithException() throws Exception {
    exception.expect(MessagingException.class);
    exception.expectCause(is(instanceOf(Throwable.class)));
    runFlow("fail-operation-with-not-handled-exception");
  }

  @Test
  public void retryPolicyNotExecutedDueToNotConnectionExceptionWithThrowable() throws Throwable {
    exception.expect(MessagingException.class);
    exception.expectCause(is(instanceOf(Throwable.class)));
    runFlow("fail-operation-with-not-handled-throwable");

  }

  @Extension(name = "petstore", description = "PetStore Test connector")
  @Operations(PetStoreOperationsWithFailures.class)
  @ConnectionProviders({PooledPetStoreConnectionProviderWithFailureInvalidConnection.class,
      PooledPetStoreConnectionProviderWithValidConnection.class})
  @Xml(namespace = "http://www.mulesoft.org/schema/mule/petstore", prefix = "petstore")
  public static class PetStoreConnectorWithConnectionFailure extends PetStoreConnector {
  }

  @Alias("valid")
  public static class PooledPetStoreConnectionProviderWithValidConnection extends PetStoreConnectionProvider<PetStoreClient>
      implements ConnectionProvider<PetStoreClient> {

  }

  @Alias("invalid")
  public static class PooledPetStoreConnectionProviderWithFailureInvalidConnection
      extends PetStoreConnectionProvider<PetStoreClient> implements ConnectionProvider<PetStoreClient> {

    @Override
    public ConnectionValidationResult validate(PetStoreClient connection) {
      return ConnectionValidationResult.failure(CONNECTION_FAIL, new Exception("Invalid credentials"));
    }
  }

  public static class PetStoreOperationsWithFailures extends PetStoreOperations {

    public Integer failConnection(@Connection PetStoreClient client) throws ConnectionException {
      throw new ConnectionException(CONNECTION_FAIL);
    }

    public Integer failOperationWithException(@Connection PetStoreClient client) throws Exception {
      throw new Exception(CONNECTION_FAIL);
    }

    public Integer failOperationWithThrowable(@Connection PetStoreClient client) throws Throwable {
      throw new Throwable(CONNECTION_FAIL);
    }
  }


}