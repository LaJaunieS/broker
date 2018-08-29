::imports the brokers certificate in the client's trust store

keytool -importcert -noprompt -alias client-key-store -file broker.cert ^
-storetype JCEKS -keystore clientTrust.jck ^
-storepass clientTrustPass