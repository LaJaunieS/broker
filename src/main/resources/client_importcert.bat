::imports the client certificate in the broker's trust store

keytool -importcert -noprompt -alias broker-key-store -file client.cert ^
-storetype JCEKS -keystore brokerTrust.jck ^
-storepass brokerTrustPass