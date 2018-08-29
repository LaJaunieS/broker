
:: generates a key pair *broker-key-store* and stores in a keystore brokerKey.jck
keytool -genkeypair -alias brokerPrivKey -keyalg RSA -keysize 512 ^
-storetype JCEKS -keystore brokerKey.jck ^
-storepass brokerStorePass -keypass brokerPrivKeyPass ^
-dname "cn=slajaunie, ou=cp130, o=uw,l=seattle,st=washington,c=us"

::Reads from keystore brokerKey the associated public key  and stores in file broker.cert
keytool -exportcert -alias brokerPrivKey -file broker.cert -storetype JCEKS ^
-keystore brokerKey.jck -storepass brokerStorePass

:: generates a key pair *client-trust-store* and stores in a keystore clientTrust.jck

keytool -genkeypair -alias client-trust-store -keyalg RSA -keysize 512 ^
-storetype JCEKS -keystore clientTrust.jck ^
-storepass clientTrustPass ^
-dname "cn=slajaunie, ou=cp130, o=uw,l=seattle,st=washington,c=us"

:: imports the brokers certificate in the client's trust store

keytool -importcert -noprompt -alias brokerCert -file broker.cert ^
-storetype JCEKS -keystore clientTrust.jck ^
-storepass clientTrustPass

:: generates a key pair *client-key-store* and stores in a keystore  clientKey.jck

keytool -genkeypair -alias clientPrivKey -keyalg RSA -keysize 512 ^
-storetype JCEKS -keystore clientKey.jck ^
-storepass clientStorePass -keypass clientPrivKeyPass ^
-dname "cn=slajaunie, ou=cp130, o=uw,l=seattle,st=washington,c=us"

:: Reads from keystore client-key-store the associated public key and stores in file broker.cert

keytool -exportcert -alias clientPrivKey -file client.cert -storetype JCEKS ^
-keystore clientKey.jck -storepass clientStorePass

:: generates a key pair *broker-trust-store* and stores in a keystore brokerTrust.jck

keytool -genkeypair -alias broker-trust-store -keyalg RSA -keysize 512 ^
-storetype JCEKS -keystore brokerTrust.jck ^
-storepass brokerTrustPass ^
-dname "cn=slajaunie, ou=cp130, o=uw,l=seattle,st=washington,c=us"

:: imports the client certificate in the broker's trust store 

keytool -importcert -noprompt -alias clientCert -file client.cert ^
-storetype JCEKS -keystore brokerTrust.jck ^
-storepass brokerTrustPass
