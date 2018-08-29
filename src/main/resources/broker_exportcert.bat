::Reads from keystore brokerKey the associated certificate and stores in file broker.cert

keytool -exportcert -alias broker-key-store -file broker.cert -storetype JCEKS ^
-keystore brokerKey.jck -storepass brokerStorePass