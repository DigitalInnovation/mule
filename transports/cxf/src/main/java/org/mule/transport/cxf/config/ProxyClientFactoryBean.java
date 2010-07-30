package org.mule.transport.cxf.config;

import org.mule.transport.cxf.CxfOutboundMessageProcessor;
import org.mule.transport.cxf.builder.ProxyClientMessageProcessorBuilder;

import org.springframework.beans.factory.FactoryBean;

public class ProxyClientFactoryBean extends ProxyClientMessageProcessorBuilder implements FactoryBean
{

    public Object getObject() throws Exception
    {
        return build();
    }

    public Class getObjectType()
    {
        return CxfOutboundMessageProcessor.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

}
