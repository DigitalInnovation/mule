package org.mule.transport.cxf.builder;

import org.mule.api.lifecycle.CreateException;
import org.mule.transport.cxf.CxfConstants;
import org.mule.transport.cxf.CxfOutboundMessageProcessor;
import org.mule.transport.cxf.support.CopyAttachmentInInterceptor;
import org.mule.transport.cxf.support.CopyAttachmentOutInterceptor;
import org.mule.transport.cxf.support.CxfUtils;
import org.mule.transport.cxf.support.OutputPayloadInterceptor;
import org.mule.transport.cxf.support.ProxyService;
import org.mule.transport.cxf.support.ResetStaxInterceptor;
import org.mule.transport.cxf.support.ReversibleStaxInInterceptor;
import org.mule.transport.cxf.support.StreamClosingInterceptor;
import org.mule.transport.cxf.transport.MuleUniversalConduit;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap11FaultInInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap12FaultInInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.databinding.stax.StaxDataBinding;
import org.apache.cxf.databinding.stax.StaxDataBindingFeature;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.interceptor.WrappedOutInterceptor;

/**
 * Creates an outbound proxy based on a specially configure CXF Client.
 * This allows you to send raw XML to your MessageProcessor and have it sent
 * through CXF for SOAP processing, WS-Security, etc.
 * <p>
 * The input to the resulting MessageProcessor can be either a SOAP Body
 * or a SOAP Envelope depending on how the payload attribute is configured.
 * Valid values are "body" or "envelope". 
 */
public class ProxyClientMessageProcessorBuilder extends AbstractOutboundMessageProcessorBuilder
{
    private String payload;
    
    protected void configureClient(Client client)
    {
        MuleUniversalConduit conduit = (MuleUniversalConduit)client.getConduit();

        // add interceptors to handle Mule proxy specific stuff
        client.getInInterceptors().add(new CopyAttachmentInInterceptor());
        client.getInInterceptors().add(new StreamClosingInterceptor());
        client.getOutInterceptors().add(new OutputPayloadInterceptor());
        client.getOutInterceptors().add(new CopyAttachmentOutInterceptor());
        
        // Don't close the input because people need to be able to work with the live stream
        conduit.setCloseInput(false);
    }

    public boolean isProxyEnvelope()
    {
        return CxfConstants.PAYLOAD_ENVELOPE.equals(payload);
    }
    
    protected void configureMessageProcessor(CxfOutboundMessageProcessor processor)
    {
        processor.setProxy(true);
    }

    @Override
    protected Client createClient() throws CreateException, Exception
    {
        ClientProxyFactoryBean cpf = new ClientProxyFactoryBean();
        cpf.setServiceClass(ProxyService.class);
        cpf.setDataBinding(new StaxDataBinding());
        cpf.getFeatures().add(new StaxDataBindingFeature());
        cpf.setAddress(getAddress());
        cpf.setBus(getBus());
        cpf.setProperties(properties);
        
        if (wsdlLocation != null) 
        {
            cpf.setWsdlLocation(wsdlLocation);
        }
        
        Client client = ClientProxy.getClient(cpf.create());

        Binding binding = client.getEndpoint().getBinding();
        CxfUtils.removeInterceptor(binding.getOutInterceptors(), WrappedOutInterceptor.class.getName());
        CxfUtils.removeInterceptor(binding.getInInterceptors(), Soap11FaultInInterceptor.class.getName());
        CxfUtils.removeInterceptor(binding.getInInterceptors(), Soap12FaultInInterceptor.class.getName());
        CxfUtils.removeInterceptor(binding.getInInterceptors(), CheckFaultInterceptor.class.getName());

        if (isProxyEnvelope()) 
        {
            CxfUtils.removeInterceptor(binding.getOutInterceptors(), SoapOutInterceptor.class.getName());
            client.getInInterceptors().add(new ReversibleStaxInInterceptor());
            client.getInInterceptors().add(new ResetStaxInterceptor());
        }
        
        return client;
    }

    public String getPayload()
    {
        return payload;
    }

    public void setPayload(String payload)
    {
        this.payload = payload;
    }
    
}
