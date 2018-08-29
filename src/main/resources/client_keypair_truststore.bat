::generates a key pair *client-trust-store* and stores in a keystore clientTrust.jck

keytool -genkeypair -alias client-trust-store -keyalg RSA -keysize 512 ^
-storetype JCEKS -keystore clientTrust.jck ^
-storepass clientTrustPass ^
-dname "cn=slajaunie, ou=cp130, o=uw,l=seattle,st=washington,c=us"


