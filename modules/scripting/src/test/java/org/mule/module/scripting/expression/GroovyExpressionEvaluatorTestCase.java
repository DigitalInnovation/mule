/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.scripting.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.transport.PropertyScope;
import org.mule.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;

import java.util.Arrays;

import groovyjarjarasm.asm.ClassWriter;
import groovyjarjarasm.asm.Opcodes;

import org.junit.Test;
import org.mockito.Mockito;

public class GroovyExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testWithExpressions()
    {
        FruitBowl payload = createFruitBowl();
        DefaultMuleMessage msg = new DefaultMuleMessage(payload, muleContext);
        GroovyExpressionEvaluator e = new GroovyExpressionEvaluator();
        e.setMuleContext(muleContext);
        Object value = e.evaluate("payload.apple.washed", msg);
        assertNotNull(value);
        assertTrue(value instanceof Boolean);
        assertTrue((Boolean) value);

        value = e.evaluate("message.payload.banana.bitten", msg);
        assertNotNull(value);
        assertTrue(value instanceof Boolean);
        assertTrue((Boolean) value);
    }

    @Test
    public void stringToPrimitiveIntTypeConversion()
    {
        DefaultMuleMessage msg = new DefaultMuleMessage("1", muleContext);
        GroovyExpressionEvaluator e = new GroovyExpressionEvaluator();
        e.setMuleContext(muleContext);

        Object value = e.evaluate("(payload as int) > 0", msg);
        assertTrue(value instanceof Boolean);
        assertTrue((Boolean) value);

        Object value2 = e.evaluate("(payload as int) > 1", msg);
        assertTrue(value2 instanceof Boolean);
        assertFalse((Boolean) value2);

        Object value3 = e.evaluate("(payload as int) == 1", msg);
        assertTrue(value3 instanceof Boolean);
        assertTrue((Boolean) value3);
    }

    @Test
    public void longNumberTypeConversion()
    {
        DefaultMuleMessage msg = new DefaultMuleMessage(new Long("1"), muleContext);
        GroovyExpressionEvaluator e = new GroovyExpressionEvaluator();
        e.setMuleContext(muleContext);

        Object value = e.evaluate("payload > 0", msg);
        assert (value instanceof Boolean);
        assertTrue((Boolean) value);

        Object value2 = e.evaluate("payload > 1", msg);
        assertTrue(value2 instanceof Boolean);
        assertFalse((Boolean) value2);

        Object value3 = e.evaluate("payload == 1", msg);
        assertTrue(value3 instanceof Boolean);
        assertTrue((Boolean) value3);
    }

    @Test
    public void primitiveLongNumberTypeConversion()
    {
        DefaultMuleMessage msg = new DefaultMuleMessage(Long.parseLong("1"), muleContext);
        GroovyExpressionEvaluator e = new GroovyExpressionEvaluator();
        e.setMuleContext(muleContext);

        Object value = e.evaluate("payload > 0", msg);
        assert (value instanceof Boolean);
        assertTrue((Boolean) value);

        Object value2 = e.evaluate("payload > 1", msg);
        assertTrue(value2 instanceof Boolean);
        assertFalse((Boolean) value2);

        Object value3 = e.evaluate("payload == 1", msg);
        assertTrue(value3 instanceof Boolean);
        assertTrue((Boolean) value3);
    }

    @Test(expected = MuleRuntimeException.class)
    public void testThrowsExceptionOnScriptError() throws Exception
    {
        FruitBowl payload = createFruitBowl();
        DefaultMuleMessage msg = new DefaultMuleMessage(payload, muleContext);

        GroovyExpressionEvaluator e = new GroovyExpressionEvaluator();
        e.setMuleContext(muleContext);
        e.evaluate("unexistent", msg);
    }

    @Test
    public void testRegistrySyntax() throws Exception
    {
        Apple apple = new Apple();
        muleContext.getRegistry().registerObject("name.with.dots", apple);
        Object result = muleContext.getExpressionManager().evaluate(
            "#[groovy:registry.lookupObject('name.with.dots')]", Mockito.mock(MuleMessage.class));

        assertNotNull(result);
        assertSame(apple, result);

        // try various map-style access in groovy for simpler syntax
        result = muleContext.getExpressionManager().evaluate("#[groovy:registry.'name.with.dots']",
            Mockito.mock(MuleMessage.class));
        assertNotNull(result);
        assertSame(apple, result);

        result = muleContext.getExpressionManager().evaluate("#[groovy:registry['name.with.dots']]",
            Mockito.mock(MuleMessage.class));
        assertNotNull(result);
        assertSame(apple, result);

        result = muleContext.getExpressionManager().evaluate("#[groovy:registry.'name.with.dots'.washed]",
            Mockito.mock(MuleMessage.class));
        assertNotNull(result);
        assertEquals(false, result);
    }

    @Test
    public void muleContext() throws Exception
    {
        assertSame(
            muleContext,
            muleContext.getExpressionManager().evaluate("#[groovy:muleContext]",
                Mockito.mock(MuleMessage.class)));
    }

    @Test
    public void message() throws Exception
    {
        MuleMessage mockMessage = Mockito.mock(MuleMessage.class);
        assertSame(mockMessage, muleContext.getExpressionManager().evaluate("#[groovy:message]", mockMessage));
    }

    @Test
    public void payload() throws Exception
    {
        MuleMessage mockMessage = Mockito.mock(MuleMessage.class);
        Object payload = new Object();
        Mockito.when(mockMessage.getPayload()).thenReturn(payload);
        assertSame(payload, muleContext.getExpressionManager().evaluate("#[groovy:payload]", mockMessage));
    }

    @Test
    public void src() throws Exception
    {
        MuleMessage mockMessage = Mockito.mock(MuleMessage.class);
        Object payload = new Object();
        Mockito.when(mockMessage.getPayload()).thenReturn(payload);
        assertSame(payload, muleContext.getExpressionManager().evaluate("#[groovy:src]", mockMessage));
    }

    @Test
    public void inboundProperty() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setProperty("foo", "bar", PropertyScope.INBOUND);
        assertEquals("bar", muleContext.getExpressionManager().evaluate("#[groovy:inbound['foo']]", message));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignInboundProperty() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        muleContext.getExpressionManager().evaluate("#[groovy:inbound['foo']='bar']", message);
        assertEquals("bar", message.getInboundProperty("foo"));
    }

    @Test
    public void outboundProperty() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setOutboundProperty("foo", "bar");
        assertEquals("bar", muleContext.getExpressionManager().evaluate("#[groovy:outbound['foo']]", message));
    }

    @Test
    public void assignValueToOutboundProperty() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setOutboundProperty("foo", "bar_old");
        muleContext.getExpressionManager().evaluate("#[groovy:outbound['foo']='bar']", message);
        assertEquals("bar", message.getOutboundProperty("foo"));
    }

    @Test
    public void assignValueToNewOutboundProperty() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        muleContext.getExpressionManager().evaluate("#[groovy:outbound['foo']='bar']", message);
        assertEquals("bar", message.getOutboundProperty("foo"));
    }

    @Test
    public void variableFromFlowScope() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar");
        event.setSessionVariable("foo", "NOTbar");
        assertEquals(event.getFlowVariable("foo"),
            muleContext.getExpressionManager().evaluate("#[groovy:foo]", message));
    }

    @Test
    public void variableFromSessionScope() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setSessionVariable("foo", "bar");
        assertEquals(event.getSessionVariable("foo"),
            muleContext.getExpressionManager().evaluate("#[groovy:foo]", message));
    }

    @Test
    public void assignValueToVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar_old");
        muleContext.getExpressionManager().evaluate("#[groovy:foo='bar']", message);
        // New value is not assigned.
        assertEquals("bar_old", event.getFlowVariable("foo"));
    }

    @Test
    public void assignValueToNewVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        muleContext.getExpressionManager().evaluate("#[groovy:foo='bar']", message);
        // Value is not assigned, not sure why this doesn't fail.
        assertNull(event.getFlowVariable("foo"));
    }

    @Test
    public void flowVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar");
        assertEquals(event.getFlowVariable("foo"),
            muleContext.getExpressionManager().evaluate("#[groovy:flowVariables['foo']]", message));
    }

    @Test
    public void assignValueToFlowVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar_old");
        muleContext.getExpressionManager().evaluate("#[groovy:flowVariables['foo']='bar']", message);
        assertEquals("bar", event.getFlowVariable("foo"));
    }

    @Test
    public void assignValueToNewFlowVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        muleContext.getExpressionManager().evaluate("#[groovy:flowVariables['foo']='bar']", message);
        assertEquals("bar", event.getFlowVariable("foo"));
    }

    @Test
    public void sessionVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setSessionVariable("foo", "bar");
        assertEquals(event.getSessionVariable("foo"),
            muleContext.getExpressionManager().evaluate("#[groovy:sessionVariables['foo']]", message));
    }

    @Test
    public void assignValueToSessionVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setSessionVariable("foo", "bar_old");
        muleContext.getExpressionManager().evaluate("#[groovy:sessionVariables['foo']='bar']", message);
        assertEquals("bar", event.getSessionVariable("foo"));
    }

    @Test
    public void assignValueToNewSessionVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        muleContext.getExpressionManager().evaluate("#[groovy:sessionVariables['foo']='bar']", message);
        assertEquals("bar", event.getSessionVariable("foo"));
    }

    @Test
    public void exception() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        RuntimeException rte = new RuntimeException();
        message.setExceptionPayload(new DefaultExceptionPayload(rte));
        assertEquals(rte, muleContext.getExpressionManager().evaluate("#[groovy:exception]", message));
    }

    @Test
    public void testComplexExpressionLowLevelParsing() throws Exception
    {
        final GroovyExpressionEvaluator evaluator = new GroovyExpressionEvaluator();
        evaluator.setMuleContext(muleContext);
        muleContext.getExpressionManager().registerEvaluator(evaluator);

        MuleMessage msg = new DefaultMuleMessage(Arrays.asList(0, "test"), muleContext);
        String result = muleContext.getExpressionManager().parse(
            "#[groovy:payload[0]] - #[groovy:payload[1].toUpperCase()]", msg);

        assertNotNull(result);
        assertEquals("Expressions didn't evaluate correctly", "0 - TEST", result);
    }

    /**
     * See: MULE-4797 GroovyExpressionEvaluator script is unable to load user classes when used with hot
     * deployment See:
     */
    @Test
    public void testUseContextClassLoaderToResolveClasses() throws ClassNotFoundException
    {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(new MyClassClassLoader());
            assertFalse((Boolean) muleContext.getExpressionManager().evaluate(
                "groovy:payload instanceof MyClass", new DefaultMuleMessage("test", muleContext)));
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private FruitBowl createFruitBowl()
    {
        Apple apple = new Apple();
        apple.wash();
        Banana banana = new Banana();
        banana.bite();

        return new FruitBowl(apple, banana);
    }

    class MyClassClassLoader extends ClassLoader
    {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException
        {
            if (name.equals("MyClass"))
            {
                ClassWriter cw = new ClassWriter(true);
                cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "MyClass", null, "java/lang/Object", null);
                return defineClass(name, cw.toByteArray(), 0, cw.toByteArray().length);
            }
            else
            {
                return super.findClass(name);
            }
        }
    }
}
