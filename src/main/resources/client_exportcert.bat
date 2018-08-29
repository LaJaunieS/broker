::Reads from keystore client-key-store the associated certificate and stores in file broker.cert

keytool -exportcert -alias client-key-store -file client.cert -storetype JCEKS ^
-keystore clientKey.jck -storepass clientStorePass