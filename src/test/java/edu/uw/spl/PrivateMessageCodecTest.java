package edu.uw.spl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.uw.ext.framework.crypto.PrivateMessageTriple;
import edu.uw.ext.framework.order.ClientOrder;
import edu.uw.ext.framework.crypto.PrivateMessageCodec;

/**
 * Test an implementation of PrivateMessageCodec.
 * @author Russ Moul
 */
public class PrivateMessageCodecTest {
    /** This class' logger. */
    private static final Logger log = 
                         LoggerFactory.getLogger(PrivateMessageCodecTest.class);

    /** Spring bean factory. */
    private ClassPathXmlApplicationContext appContext;

    @Test
    public void codecTest() throws Exception {
        appContext = new ClassPathXmlApplicationContext("context.xml");
        PrivateMessageCodec codec = appContext.getBean("PrivateMessageCodec", PrivateMessageCodec.class);

        ClientOrder order1 = new ClientOrder("tony", ClientOrder.Type.BUY, "TYRL", 10);
        ClientOrder order2 = new ClientOrder("pepper", ClientOrder.Type.BUY, "CDYN", 20, 5000);
        ClientOrder order3 = new ClientOrder("tony", ClientOrder.Type.SELL, "CDYN", 10);
        ClientOrder order4 = new ClientOrder("pepper", ClientOrder.Type.SELL, "TYRL", 20, 7000);

        List<ClientOrder> orders = new ArrayList<>();
        orders.add(order1);
        orders.add(order2);
        orders.add(order3);
        orders.add(order4);
        
        ObjectMapper mapper = new ObjectMapper();
        // could go straight to bytes but want to log for visibility
        //byte[] data  = mapper.writeValueAsBytes(orders);
        String origPlaintext = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(orders);
        log.info(String.format("Orig plaintext:%n%s", origPlaintext));
        byte[] data = origPlaintext.getBytes();
        
        // Parameters for getting the client private key
        String clientKeyStoreName = "clientKey.jck";
        char[] clientStorePasswd = "clientStorePass".toCharArray();
        String signerKeyName = "clientPrivKey";
        char[] signerPasswd =  "clientPrivKeyPass".toCharArray();
        
        // Parameters for getting the brokers certificate (public key)
        String clientTrustStoreName = "clientTrust.jck";
        char[] clientTrustStorePasswd = "clientTrustPass".toCharArray();
        String brokerCertName = "brokerCert";

        PrivateMessageTriple message = 
        codec.encipher(data,
                       clientKeyStoreName, clientStorePasswd,
                       signerKeyName, signerPasswd, 
                       clientTrustStoreName, clientTrustStorePasswd, brokerCertName);

        // Parameters for getting the broker private key
        String brokerKeyStoreName = "brokerKey.jck";
        char[] brokerStorePasswd = "brokerStorePass".toCharArray();
        String brokerPrivKeyName = "brokerPrivKey";
        char[] brokerPrivKeyPasswd =  "brokerPrivKeyPass".toCharArray();

        // Parameters for getting the client certificate (public key)
        String brokerTrustStoreName = "brokerTrust.jck";
        char[] brokerTrustStorePasswd = "brokerTrustPass".toCharArray();
        String clientCertName = "clientCert";
        
        byte[] plaintext =  codec.decipher(message,
                                           brokerKeyStoreName, brokerStorePasswd, 
                                           brokerPrivKeyName, brokerPrivKeyPasswd,

                                           brokerTrustStoreName, brokerTrustStorePasswd,
                                           clientCertName);
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, ClientOrder.class);
        try {
            List<ClientOrder> read = mapper.readValue(plaintext, type);

            assertEquals(order1, read.get(0));
            assertEquals(order2, read.get(1));
            assertEquals(order3, read.get(2));
            assertEquals(order4, read.get(3));

        } catch (IOException e) {
            throw new IOException("Error parsing order data.", e);
        }
    }
}
