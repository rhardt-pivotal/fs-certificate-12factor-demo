# File System 12 Factor Demo

## Introduction

> Some Java apps use certificates to authenticate a server but do not delegate to the JDK's cacerts file. They rather expect to be pointed to a .pem or .der-encoded file on the local filesystem.  This is incompatible with 12 factor principles.  

> This is an example of a Spring Boot app that takes the PEM-encoded content of a server certificate as an environment variable, writes it to a temp file, and makes the location of that temp file available as a property Spring's application context, thereby bridging 12-factor runtime environment with the legacy client's need for a local file.

## Code Samples

```
@Component
public class CertVerifier {
     @Value("${SSLTrustedCertificate}")
     public String outputCert(String hostName, int port) {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            InputStream is = new BufferedInputStream( new FileInputStream (certFileLoc));
            X509Certificate cert = (X509Certificate) fact.generateCertificate(is);
            return cert.toString(); 
      }    
}
```

## Installation

```
export CERT_CONTENTS="-----BEGIN CERTIFICATE-----
MIIGQDCCBCigAwIBAgICEAQwDQYJKoZIhvcNAQELBQAwbjELMAkGA1UEBhMCVVMx
DjAMBgNVBAgMBVRleGFzMQ8wDQYDVQQKDAZyaGFyZHQxCzAJBgNVBAsMAml0MQ8w
etc.etc.-----END CERTIFICATE-----"

./mvnw clean package
./run.sh
```
