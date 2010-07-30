package org.mule.transport.cxf.endpoint;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.AbstractMetaEndpointBuilder;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.cxf.builder.LocalClientMessageProcessorBuilder;

import java.util.ArrayList;

public class CxfEndpointBuilder extends AbstractMetaEndpointBuilder
{

    public CxfEndpointBuilder()
    {
        super();
    }

    public CxfEndpointBuilder(EndpointURI endpointURI)
    {
        super(endpointURI);
    }

    public CxfEndpointBuilder(EndpointURIEndpointBuilder global) throws EndpointException
    {
        super(global);
    }

    public CxfEndpointBuilder(ImmutableEndpoint source)
    {
        super(source);
    }

    public CxfEndpointBuilder(String address, MuleContext muleContext)
    {
        super(getEndpointAddressWithoutMetaScheme(address), muleContext);
    }

    public CxfEndpointBuilder(URIBuilder builder)
    {
        super(builder);
    }

    @Override
    public InboundEndpoint buildInboundEndpoint() throws EndpointException, InitialisationException
    {
        throw new UnsupportedOperationException("Inbound meta CXF endpoints not supported");
    }

    @Override
    public OutboundEndpoint buildOutboundEndpoint() throws EndpointException, InitialisationException
    {
        LocalClientMessageProcessorBuilder builder = new LocalClientMessageProcessorBuilder();
        builder.setMuleContext(muleContext);
        builder.setAddress(getEndpointBuilder().getEndpoint().toString());
        
        try
        {
            ArrayList<MessageProcessor> processors = new ArrayList<MessageProcessor>();
            processors.add(builder.build());
            if (messageProcessors != null) 
            {
                processors.addAll(messageProcessors);
            }
            messageProcessors = processors;
        }
        catch (Exception e)
        {
            throw new EndpointException(e);
        }

        return super.buildOutboundEndpoint();
    }

}
