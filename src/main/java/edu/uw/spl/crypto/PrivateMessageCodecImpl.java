package edu.uw.spl.crypto;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.crypto.PrivateMessageCodec;
import edu.uw.ext.framework.crypto.PrivateMessageTriple;

public class PrivateMessageCodecImpl implements PrivateMessageCodec {

    private static final Logger log = LoggerFactory.getLogger(PrivateMessageCodecImpl.class);
    
    
    /**
     * Decipher the provided message.
     * Keystores will be accessed as resources, i.e. on the classpath.
     *
     * @param triple the message containing the ciphertext, key and signature
     * 
     * @param recipientKeyStoreName the name of the recipient's key store resource
     * @param recipientKeyStorePasswd the recipient's key store password
     * @param recipientKeyName the alias of the recipient's private key
     * @param recipientKeyPasswd the password for the recipient's private key
     * 
     * @param trustStoreName the name of the trust store resource
     * @param trustStorePasswd the trust store password
     * @param signerCertName the name of the signer's certificate
     * 
     * @return the plaintext from the file
     * @throws GeneralSecurityException if any cryptographic operations fail
     * @throws IOException if unable to write either of the files
     */
    @Override
    public byte[] decipher(PrivateMessageTriple triple, 
                            String recipientKeyStoreName,
                            char[] recipientKeyStorePassword,
                            String recipientKeyName,
                            char[] recipientKeyPassword,
                            String trustStoreName,
                            char[] trustStorePassword,
                            String signerCertName) 
                                    throws GeneralSecurityException, IOException {
        // TODO Auto-generated method stub
        /*Obtain the shared secret key, order data ciphertext, and signature from
         * provided PrivateMessageTriple
         */
        byte[] encipheredSharedKey = triple.getEncipheredSharedKey();
        byte[] cipherText = triple.getCiphertext();
        byte[] signature = triple.getSignature();
        byte[] decryptedData = null;
        /*Get the keystore containing the private key*/
        try (FileInputStream fis = new FileInputStream("src/main/resources/" + recipientKeyStoreName)){
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            keyStore.load(fis, recipientKeyStorePassword);
            log.info("Sucessfully obtained keystore data");
            
            /*Retrieve the broker's private key from the provided keystore*/
            Key brokerPrivateKey = keyStore.getKey(recipientKeyName, recipientKeyPassword);
            log.info("Successfully obtained private key ",recipientKeyName);
            
            /*Use the private key from the keystore to decipher the shared secret key's bytes*/
            Cipher decipher = Cipher.getInstance(brokerPrivateKey.getAlgorithm());
            decipher.init(Cipher.DECRYPT_MODE, brokerPrivateKey);
            log.info("Private key initialized: {}",brokerPrivateKey.getFormat());
            //VVThis isn't right
            //decryptedData = decipher.doFinal(cipherText);
            //log.info("Successfully deciphered shared secret key");
            
            
            /*Reconstruct the shared secret key from shared secret key's bytes*/
            
            /*Use the shared secret key to decipher the order data ciphertext*/
            SecretKey sharedSecretKey = new SecretKeySpec(encipheredSharedKey,"AES");
            Cipher decipheredData = Cipher.getInstance(sharedSecretKey.getAlgorithm());
            decipheredData.init(Cipher.DECRYPT_MODE, sharedSecretKey);
            log.info("Sucessfully initialized cipher for shared secret key algorith: {}"
                    ,sharedSecretKey.getAlgorithm());
            
            decryptedData = decipheredData.doFinal(cipherText);
            log.info("Successfullly deciphered cipher text data");
        
            return decryptedData;
            
        } catch (FileNotFoundException ex) {
            log.warn("Unable to locate file {}",recipientKeyStoreName,ex);
        } catch (UnrecoverableKeyException ex) {
            log.warn("The password for the private key/key store was incorrect",ex);
        } catch (IOException ex){
            log.warn("Unable to load {}",recipientKeyStoreName,ex);
        }
        if (decryptedData == null) {
            log.info("Unable to return deciphered data");
        }
        return decryptedData;    
        /*Retrieve the (client's) public key from the provided truststore*/
        
        /*Verify the order data plaintext and signature using the public key from the truststore*/
        
        /*Return the order data plaintext*/
    }
    
    
    /**
     * Writes the client order file.  
     *
     * Key stores will be accessed as resources, i.e. on the classpath.
     * 
     * @param plaintext the data to be encrypted
     * 
     * @param senderKeyStoreName the name of the sender's key store resource
     * @param senderKeyStorePasswd the sender's key store password
     * @param senderKeyName the alias of the sender's private key
     * @param senderKeyPasswd the password for the sender's private key
     * 
     * @param senderTrustStoreName the name of the sender's trust key store resource
     * @param senderTrustStorePasswd the sender's trust store key
     * @param recipientCertName the alias of the recipient's certificate key
     * 
     * @return message containing the ciphertext, key and signature
     * @throws GeneralSecurityException if any cryptographic operations fail
     * @throws IOException if unable to write either of the files
     */
    @Override
    public PrivateMessageTriple encipher(byte[] plaintext,
                                        String senderKeyStoreName,
                                        char[] senderKeyStorePassword,
                                        String senderKeyName,
                                        char[] senderKeyPassword,
                                        String senderTrustStoreName,
                                        char[] senderTrustStorePassword,
                                        String recipientCertName)
                                                throws GeneralSecurityException, IOException {
        Signature signer = null;
        byte[] signatureBytes; 
        PrivateKey privateKey = null;
        PublicKey publicKey = null;
        byte[] encipheredSharedKey = null;
        byte[] encryptedData = null;
        
        
        
        
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        
        /*Load the keystore data and the private key*/
        try (FileInputStream fis = new FileInputStream("src/main/resources/" + senderKeyStoreName)){
            log.info("Sucessfully loaded {}",senderKeyStoreName);
            keyStore.load(fis, senderKeyStorePassword);
            
            /*Retrieve the client's private key from the provided keystore*/
            if (keyStore.containsAlias(senderKeyName)) {
                privateKey = (PrivateKey) keyStore.getKey(senderKeyName, senderKeyPassword);
            } else {
                log.warn("Unable to locate private key for {}",senderKeyName);
            }
        } catch (FileNotFoundException ex) {
            log.warn("Unable to locate file {}",senderKeyStoreName,ex);
        } catch (IOException ex){
            log.warn("Unable to load {}",senderKeyStoreName,ex);
        } catch (UnrecoverableKeyException ex) {
            log.warn("The given password was incorrect",ex);
        }
        
        
        /*Load the truststore data and the public key...*/
        try (FileInputStream fis = new FileInputStream("src/main/resources/" + senderTrustStoreName)){
            log.info("Sucessfully loaded {}",senderTrustStoreName);
            
            KeyStore trustStore = KeyStore.getInstance("JCEKS");
            Certificate certificate = null;
            trustStore.load(fis, senderTrustStorePassword);
            if (trustStore.containsAlias(recipientCertName)) {
                if (trustStore.isCertificateEntry(recipientCertName)) {
                    certificate = trustStore.getCertificate(recipientCertName);
                    publicKey = certificate.getPublicKey();
                    
                } else {
                    publicKey = (PublicKey) trustStore.getKey(recipientCertName, senderTrustStorePassword);
                }
                
                /*...Now generate the symmetric secret key...*/
                KeyGenerator generator = KeyGenerator.getInstance("AES");
                generator.init(128);
                SecretKey secretKey = generator.generateKey();
            
                /*...And encipher the data with symmetric secret key*/
                Cipher cipher = Cipher.getInstance(secretKey.getAlgorithm());
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                encryptedData = cipher.doFinal(plaintext);
                /*Get the bytes for the secret key...*/
                encipheredSharedKey = secretKey.getEncoded();
            
                /*...And encipher the shared symmetric secret key's bytes using the 
                 * public key from the truststore*/
                cipher = Cipher.getInstance(publicKey.getAlgorithm());
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            
            } else {
                log.warn("Unable to locate public key {}",recipientCertName);
            }
            
            
        } catch (FileNotFoundException ex) {
            log.warn("Unable to locate file {}",senderKeyStoreName,ex);
        } catch (IOException ex){
            log.warn("Unable to load {}",senderKeyStoreName,ex);
        } catch (UnrecoverableKeyException ex) {
            log.warn("The given password was incorrect",ex);
        }
        
        
        /*Separately, sign the data*/
        signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(plaintext);
        signatureBytes = signer.sign();
        
        /*Create the new private message with the public key, the data, and the signature*/
        return new PrivateMessageTriple(encipheredSharedKey, encryptedData, signatureBytes);
    }

}
