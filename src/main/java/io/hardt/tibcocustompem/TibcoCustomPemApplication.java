package io.hardt.tibcocustompem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.PostConstruct;
import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;


/**
 * Spring boot application for a wonky client that doesn't use cacerts.jks for its truststore
 * but rather wants a path to a .der or .pem on the local filesystem.
 *
 * This allows you to set an environment variable to CERT_CONTENTS,
 * and this app will write those contents to a tempfile and expose
 * the location of the tempfile as a property to the spring application context
 *
 * This allows for 12-factor friendly deployments where the certificate
 * contents are exposed as an environment variable at runtime rather
 * than a file embedded in the code
 *
 */
@SpringBootApplication
public class TibcoCustomPemApplication {

	private static final String CERT_CONTENTS_PROPERTY_NAME="cert.contents";
	private static final String GENERATED_PEMFILE_LOC_PROPERTY_NAME = "tibco.ems.SSLTrustedCertificate";

	public static void main(String[] args) {
		SpringApplication.run(TibcoCustomPemApplication.class, args);
	}

	@Value("${cert.contents}")
	private String certPem;

	@Autowired
	private ConfigurableEnvironment env;

	@PostConstruct
	private void init() {
		env.getPropertySources().addLast(new PemWriterPropertySource("TibcoPemFileLocPS", certPem));
	}

	class PemWriterPropertySource extends PropertySource<String> {



		private String tempTrustFileName;

		public PemWriterPropertySource(String name, String pemContents) {
			super(name);
			try{
				File f = File.createTempFile("TibcoCert", ".pem");
				Writer fw = new BufferedWriter( new FileWriter(f));
				fw.write(pemContents);
				fw.close();
				tempTrustFileName = f.getAbsolutePath();
			}
			catch (IOException ioe) {
				throw new RuntimeException("Problem reading the ");
			}

		}

		@Override
		public boolean containsProperty(String name) {
			return GENERATED_PEMFILE_LOC_PROPERTY_NAME.equals(name);
		}

		@Override
		public String getProperty(String s) {
			if(containsProperty(s)) {
				return tempTrustFileName;
			}
			return null;
		}
	}


	/**
	 * Simple web endpoint that listens on localhost:8080 by default and returns the certificate details
	 */
	@Controller
	class CustomPemController{

		@Value("${cert.contents}")
		private String certContents;

		@Value("${tibco.ems.SSLTrustedCertificate}")
		private String certFileLoc;

		@Value("${SSLTrustedCertificate}")
		private String indirect;


		@RequestMapping("/")
		public ResponseEntity<String> deaultRequest() throws IOException, CertificateException {

			File f = new File(certFileLoc);
			BufferedReader br = new BufferedReader(new FileReader(f));
			StringBuilder sb = new StringBuilder();

			br.lines().forEach(line -> sb.append(line));

			CertificateFactory fact = CertificateFactory.getInstance("X.509");
			InputStream is = new BufferedInputStream( new FileInputStream (certFileLoc));
			X509Certificate cert = (X509Certificate) fact.generateCertificate(is);

			return ResponseEntity.ok(
					"<html><body>"+
							"<p>"+CERT_CONTENTS_PROPERTY_NAME+" = <br /><pre>"+certContents+"</pre>"+
							"<br/><br/>"+
							"<p>"+GENERATED_PEMFILE_LOC_PROPERTY_NAME+" = "+certFileLoc+
							"<br/><br/>"+
							"<p>Indirect via application.properties = "+indirect+
							"<br/><br/>"+
							"<p>Cert contents read from file = <br/><pre>"+sb.toString()+"</pre>"+
							"<br/><br/> <p>decoded cert: "+cert.toString()+"</body></html>"
			);
		}

	}



}


