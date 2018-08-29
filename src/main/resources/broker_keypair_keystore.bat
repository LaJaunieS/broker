::generates a key pair *broker-key-store* and stores in a keystore brokerKey.jck

keytool -genkeypair -alias broker-key-store -keyalg RSA -keysize 512 ^
-storetype JCEKS -keystore brokerKey.jck ^
-storepass brokerStorePass -keypass brokerPrivKeyPass ^
-dname "cn=slajaunie, ou=cp130, o=uw,l=seattle,st=washington,c=us"


::Generates the following warning message- for future reference, can disregard for this assignment:
::The generated certificate uses a 512-bit RSA key which is considered a security risk.
::The JCEKS keystore uses a proprietary format. It is recommended to migrate to PKCS12 which is an industry standard format
:: using "keytool -importkeystore -srckeystore brokerKey.jck -destkeystore brokerKey.jck -deststoretype pkcs12".
::