package edu.uw.spl.crypto;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.crypto.PrivateMessageCodec;
import edu.uw.ext.framework.crypto.PrivateMessageTriple;

public class PrivateMessageCodecImpl implements PrivateMessageCodec {

    private static final Logger log = LoggerFactory.getLogger(PrivateMessageCodecImpl.class);
    
    private static final String JCEKS = "JCEKS";
    
    private static final String ALGORITHM_AES = "AES";
    
    private static final int ALGORITHM_AES_KEYSIZE = 128;
    
    private static final String SIGNATURE_TRANSFORM = "SHA256withRSA";
    
    /**Default constructor*/
    public PrivateMessageCodecImpl() {}
    
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
        
        /*Generate secret key and obtain raw bytes*/
        SecretKey sharedSecretKey = generateAesSecretKey();
        byte[] sharedSecretKeyBytes = sharedSecretKey.getEncoded();
        
        /*Get sender's KeyStore and use to obtain a public key using the recipient's certificate*/
        KeyStore senderTrustStore = loadKeyStore(senderTrustStoreName,senderTrustStorePassword);
        PublicKey publicKey = senderTrustStore.getCertificate(recipientCertName).getPublicKey();
        
        /*Encipher the public key using the secret key*/
        byte[] encipheredSharedKey = encipherData(publicKey,sharedSecretKeyBytes);
        log.info("enciphered shared key enciphered using algorithm {}",publicKey.getAlgorithm());
        /*Encipher the plaintext using the secret key*/
        byte[] cipherText = encipherData(sharedSecretKey, plaintext);
        log.info("client data enciphered using {} bit {}",
                                                        sharedSecretKeyBytes.length*8,
                                                        sharedSecretKey.getAlgorithm());
        /*Generate the signature*/
        byte[] signature = sign(plaintext, 
                                senderKeyStoreName,
                                senderKeyStorePassword,
                                senderKeyName,
                                senderKeyPassword);
        /*Instantiate a new private message using the (enciphered) sharedkey, 
         * the enciphered data, and the generated signature*/
        PrivateMessageTriple triple = new PrivateMessageTriple(encipheredSharedKey,
                                                                cipherText,
                                                                signature);
        
        log.info("Enciphered/signed data successfully");
        return triple;
        
    }

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
        /*Obtain the shared secret key, order data ciphertext, and signature from
         * provided PrivateMessageTriple
         */
        /*Obtain the key store*/
        KeyStore keyStore = loadKeyStore(recipientKeyStoreName,recipientKeyStorePassword);
        
        /*Obtain the secret key*/
        Key secretKey = keyStore.getKey(recipientKeyName, recipientKeyPassword);
        log.info("Secret key generated using: {}",secretKey.getAlgorithm());
        /*Obtain the deciphered bytes for the encphered shared key, and use to create a new
         * secret key instance*/
        byte[] decipheredSharedKeyBytes = decipherData(secretKey,
                                                    triple.getEncipheredSharedKey(),
                                                    secretKey.getAlgorithm());
        SecretKey sharedSecretKey = keyBytesToAesSecretKey(decipheredSharedKeyBytes);
        log.info("Shared secret key generated using {}",sharedSecretKey.getAlgorithm());
        /*Obtain the deciphered bytes for the enciphered data*/
        log.info("Obtained shared secret key using algorithm {}",sharedSecretKey.getAlgorithm());
        byte[] plaintext = decipherData(sharedSecretKey,
                                        triple.getCiphertext(),
                                        sharedSecretKey.getAlgorithm());
        log.info("Plaintext data deciphered successfully");
        
        /*Verify the signature before returning deciphered text*/
        boolean verified = verifySignature(plaintext, triple.getSignature(),
                trustStoreName, trustStorePassword,
               signerCertName);
        if (!verified) {
            throw new SecurityException("The given signature was not verified");
        }
        log.info("Signature verified");
        return plaintext;
        
        }
    
    /*Decipher the given bytes using the given key*/
    private static byte[] decipherData(Key key, byte[] data, String algorithm) {
        byte[] decipheredBytes = null;
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, key);
            decipheredBytes = cipher.doFinal(data);
            log.info("Cipher initialized using algorithm {}",algorithm);
            
        } catch (GeneralSecurityException e){
            log.warn("Unable to decipher with the given key or with the given data",e);
        }
        return decipheredBytes;
    }
    
    /*Generate a new SecretKey from raw bytes*/
    private static SecretKey keyBytesToAesSecretKey(byte[] keyBytes) {
        SecretKey secretKey = new SecretKeySpec(keyBytes,
                                0,
                                16,
                                ALGORITHM_AES);
        return secretKey;
    }
    
    /*Generates a secret key using a KeyGenerator*/
    private static SecretKey generateAesSecretKey() {
        KeyGenerator generator;
        SecretKey secretKey = null;
        try {
            generator = KeyGenerator.getInstance(ALGORITHM_AES);
            generator.init(ALGORITHM_AES_KEYSIZE);
            secretKey = generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            log.warn("Unable to instantiate secret key with given algorithm",e);
        }
        return secretKey;                
    }
    
    /*Verify the given signature- return true if verified, false if not*/
    private static boolean verifySignature(byte[] data, 
                                        byte[] signature,
                                        String trustStoreName,
                                        char[] trustStorePassword,
                                        String signersPublicKeyName) {
        boolean verified = false;
        
        try {
            Signature verifier = Signature.getInstance(SIGNATURE_TRANSFORM);
            KeyStore clientTrustStore = loadKeyStore(trustStoreName,trustStorePassword);
            Certificate certificate = clientTrustStore.getCertificate(signersPublicKeyName);
            PublicKey publicKey = certificate.getPublicKey();
            
            verifier.initVerify(publicKey);
            verifier.update(data);
            
            verified = verifier.verify(signature);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            log.warn("Unable to generate signature with the given algorithm",e);
        } catch (InvalidKeyException e) {
            log.warn("Unable to initiate verify operation with the given public key",e);
        } catch (SignatureException e) {
            log.warn("There was an error initializing the signature",e);
        }
        return verified;
    }    
    
    
    /*Instantiates and loads a KeyStore from a file located in the root path,
     *  then returns that KeyStore, or null if there was an exception*/
    private static KeyStore loadKeyStore(final String name, final char[] password) 
            throws KeyStoreException, NoSuchAlgorithmException {
            KeyStore keyStore = null;
            try (InputStream stream = PrivateMessageCodec.class
                                            .getClassLoader()
                                            .getResourceAsStream(name);
            ){
            if (stream == null) {
                throw new KeyStoreException("Unable to obtain input stream");
            }
            keyStore = KeyStore.getInstance("JCEKS");
            keyStore.load(stream,password);
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.warn("Unable to obtain input stream for the given store name",e);
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            log.warn("There was a problem loading the given keystore's certificate",e);
        }
            
        return keyStore;
    }
    
    
    /*Generate the cipher and encipher given plaintext*/
    private static byte[] encipherData(final Key cipherKey, final byte[] plaintext) {
        byte[] cipherText = null;
        try {
            Cipher cipher = Cipher.getInstance(cipherKey.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, cipherKey);
            cipherText = cipher.doFinal(plaintext);
        } catch (InvalidKeyException e) {
            log.warn("There was an error initializing this cipher",e);
        }catch (NoSuchAlgorithmException e) {
            log.warn("Unable to instantiate cipher with the given algorithm",e);
        } catch (GeneralSecurityException e) {
            log.warn("There was an general error enciphering the Data",e);
        } 
        return cipherText;

    }
    
    /*Signs the given data in the given key store*/
    private static byte[] sign(final byte[] data,
                                final String signerKeyStoreName, 
                                final char[] signerStorePassword,
                                final String signerName, 
                                final char[] signerPassword) throws GeneralSecurityException {
        byte[] signature = null;
        
        try {
            /*Generate the key store using the name and password*/
            KeyStore clientKeyStore = loadKeyStore(signerKeyStoreName,signerStorePassword);
            PrivateKey privateKey = (PrivateKey) clientKeyStore.getKey(signerName,signerPassword);
            
            if (privateKey == null) {
                throw new GeneralSecurityException("There was an error generating the private key");
            }
            
            Signature signer = Signature.getInstance(SIGNATURE_TRANSFORM);
            signer.initSign(privateKey);
            signer.update(data);
            signature = signer.sign();
            log.info("Signed using algorithm {}",signer.getAlgorithm());
        } catch (InvalidKeyException e){
            log.warn("Unable to initiate signature with the given key",e);
        } catch (SignatureException e) {
            log.warn("Unable to return signature- The signature may not have been initialized"
                    + "properly or was unable to process the given data",e);
        }
        
        return signature;
    }

}
