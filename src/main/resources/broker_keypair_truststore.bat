::generates a key pair *broker-trust-store* and stores in a keystore brokerTrust.jck

keytool -genkeypair -alias broker-trust-store -keyalg RSA -keysize 512 ^
-storetype JCEKS -keystore brokerTrust.jck ^
-storepass brokerTrustPass ^
-dname "cn=slajaunie, ou=cp130, o=uw,l=seattle,st=washington,c=us"

