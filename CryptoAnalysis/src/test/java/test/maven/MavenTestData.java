package test.maven;

import java.util.LinkedList;
import java.util.List;

public class MavenTestData {
	List<MavenJar> jars;
	
	public MavenTestData() {
		jars = new LinkedList<>();
		prepareData();
	}
	
	public void prepareData() {
		MavenJar jar1 = new MavenJar("de.unkrig.commons", "commons-lang", "1.2.11");
		jar1.addErrors("<de.unkrig.commons.lang.crypto.MD5: byte[] of(byte[],int,int)>", "crypto.analysis.errors.ConstraintError", 1);
		jar1.addErrors("<de.unkrig.commons.lang.crypto.MD5: byte[] of(java.io.InputStream)>", "crypto.analysis.errors.ConstraintError", 1);
		jar1.addErrors("<de.unkrig.commons.lang.crypto.MD5: byte[] of(java.io.InputStream)>", "crypto.analysis.errors.TypestateError", 1);
		jar1.addErrors("<de.unkrig.commons.lang.crypto.MD5: byte[] of(java.io.InputStream)>", "crypto.analysis.errors.ImpreciseValueExtractionError", 1);
		jar1.addErrors("<de.unkrig.commons.lang.crypto.SecretKeys: javax.crypto.SecretKey adHocSecretKey(java.io.File,char[],java.lang.String,char[])>", 
				"crypto.analysis.errors.TypestateError", 1);
		jar1.addErrors("<de.unkrig.commons.lang.crypto.SecretKeys: void saveKeyStoreToFile(java.security.KeyStore,java.io.File,char[])>", "crypto.analysis.errors.TypestateError", 2);
		jar1.addErrors("<de.unkrig.commons.lang.crypto.SecretKeys: javax.crypto.SecretKey adHocSecretKey(java.io.File,char[],java.lang.String,java.lang.String,java.lang.String,java.lang.String)>", 
				"crypto.analysis.errors.TypestateError", 1);
		jar1.addErrors("<de.unkrig.commons.lang.crypto.Encryptors: de.unkrig.commons.lang.crypto.Encryptor fromKey(java.security.Key)>", "crypto.analysis.errors.ImpreciseValueExtractionError", 1);
		jar1.addErrors("<de.unkrig.commons.lang.crypto.PasswordAuthenticationStores: de.unkrig.commons.lang.crypto.PasswordAuthenticationStore encryptPasswords(javax.crypto.SecretKey,de.unkrig.commons.lang.crypto.PasswordAuthenticationStore)>",
				"crypto.analysis.errors.IncompleteOperationError", 4);
		jar1.addErrors("<de.unkrig.commons.lang.crypto.Decryptors: de.unkrig.commons.lang.crypto.Decryptor fromKey(java.security.Key)>", "crypto.analysis.errors.ImpreciseValueExtractionError", 1);
		jars.add(jar1);
		
		MavenJar jar2 = new MavenJar("com.github.t3t5u", "common-util", "1.0.0");
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: byte[] init(javax.crypto.Cipher,int,java.security.Key,java.security.spec.AlgorithmParameterSpec)>",
				"crypto.analysis.errors.RequiredPredicateError", 8);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: java.security.KeyPair generateKeyPair(java.lang.String)>",
				"crypto.analysis.errors.TypestateError", 1);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: byte[] doDecrypt(javax.crypto.Cipher,java.security.Key,byte[])>",
				"crypto.analysis.errors.TypestateError", 1);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: byte[] doDecrypt(javax.crypto.Cipher,java.security.Key,byte[])>",
				"crypto.analysis.errors.RequiredPredicateError", 1);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: byte[] doEncrypt(javax.crypto.Cipher,java.security.Key,byte[])>",
				"crypto.analysis.errors.TypestateError", 2);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: byte[] doEncrypt(javax.crypto.Cipher,java.security.Key,byte[],byte[])>",
				"crypto.analysis.errors.TypestateError", 1);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: byte[] doEncrypt(javax.crypto.Cipher,java.security.Key,byte[],byte[])>",
				"crypto.analysis.errors.RequiredPredicateError", 1);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: byte[] doDecrypt(javax.crypto.Cipher,java.security.Key,byte[],byte[])>",
				"crypto.analysis.errors.TypestateError", 1);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: byte[] doDecrypt(javax.crypto.Cipher,java.security.Key,byte[],byte[])>",
				"crypto.analysis.errors.RequiredPredicateError", 1);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: byte[] decrypt(java.lang.String,java.security.Key,byte[])>",
				"crypto.analysis.errors.IncompleteOperationError", 1);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: byte[] encrypt(java.lang.String,java.security.Key,byte[])>",
				"crypto.analysis.errors.IncompleteOperationError", 1);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: byte[] decrypt(java.lang.String,java.security.Key,byte[],byte[])>",
				"crypto.analysis.errors.IncompleteOperationError", 1);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: byte[] encrypt(java.lang.String,java.security.Key,byte[],byte[])>",
				"crypto.analysis.errors.IncompleteOperationError", 1);
		jar2.addErrors("<com.github.t3t5u.common.util.SecurityUtils: javax.crypto.Cipher doInit(javax.crypto.Cipher,int,java.security.Key,byte[])>",
				"crypto.analysis.errors.RequiredPredicateError", 1);
		jars.add(jar2);
		
		MavenJar jar3 = new MavenJar("io.rubrica", "rubrica", "0.1.8");
		jar3.addErrors("<io.rubrica.util.OcspUtils: boolean isValidCertificate(java.security.cert.X509Certificate)>",
				"crypto.analysis.errors.TypestateError", 4);
		jar3.addErrors("<io.rubrica.sign.odf.ODFSigner: byte[] sign(byte[],java.lang.String,java.security.PrivateKey,java.security.cert.Certificate[],java.util.Properties)>",
				"crypto.analysis.errors.TypestateError", 1);
		jar3.addErrors("<io.rubrica.sign.odf.ODFSigner: byte[] sign(byte[],java.lang.String,java.security.PrivateKey,java.security.cert.Certificate[],java.util.Properties)>",
				"crypto.analysis.errors.ConstraintError", 1);
		jar3.addErrors("<io.rubrica.util.HttpClient: void disableSslChecks()>", "crypto.analysis.errors.ConstraintError", 1);
		jar3.addErrors("<io.rubrica.util.HttpClient: void disableSslChecks()>", "crypto.analysis.errors.RequiredPredicateError", 2);
		jar3.addErrors("<io.rubrica.sign.Main: void main(java.lang.String[])>",  "crypto.analysis.errors.NeverTypeOfError", 1);
		jar3.addErrors("<io.rubrica.keystore.FileKeyStoreProvider: java.security.KeyStore getKeystore(char[])>",  "crypto.analysis.errors.NeverTypeOfError", 1);
		jars.add(jar3);
		
		MavenJar jar4 = new MavenJar("com.github.kcjang", "scmutil", "1.0.2.2");
		jar4.addErrors("<com.kichang.util.SSLTool: void disableCertificateValidation()>",
				"crypto.analysis.errors.RequiredPredicateError", 2);
		jar4.addErrors("<com.kichang.util.SSLTool: void disableCertificateValidation()>",
				"crypto.analysis.errors.ConstraintError", 1);
		jar4.addErrors("<com.kichang.util.HttpsClientWithoutValidation: byte[] getHttps(java.lang.String)>",
				"crypto.analysis.errors.ConstraintError", 1);
		jar4.addErrors("<com.kichang.util.HttpsClientWithoutValidation: byte[] getHttps(java.lang.String)>",
				"crypto.analysis.errors.RequiredPredicateError", 2);
		jar4.addErrors("<com.kichang.util.HttpsClientWithoutValidation: byte[] postData(java.lang.String,java.lang.String)>",
				"crypto.analysis.errors.RequiredPredicateError", 2);
		jar4.addErrors("<com.kichang.util.HttpsClientWithoutValidation: byte[] postData(java.lang.String,java.lang.String)>",
				"crypto.analysis.errors.ConstraintError", 1);
		jar4.addErrors("<com.kichang.util.Crypto2: java.lang.String encrypt(java.lang.String,java.lang.String)>",
				"crypto.analysis.errors.RequiredPredicateError", 4);
		jar4.addErrors("<com.kichang.util.Crypto: java.lang.String encrypt(java.lang.String,java.lang.String)>",
				"crypto.analysis.errors.RequiredPredicateError", 4);
		jar4.addErrors("<com.kichang.util.Crypto: java.lang.String decrypt(java.lang.String,java.lang.String)>",
				"crypto.analysis.errors.RequiredPredicateError", 3);
		jars.add(jar4);
		
//		MavenJar jar5 = new MavenJar("com.google.code.spring-crypto-utils", "spring-crypto-utils", "1.4.0");
//		jars.add(jar5);
		
//		MavenJar jar6 = new MavenJar("com.github.emc-mongoose", "mongoose-storage-driver-atmos", "0.1.6");
//		jars.add(jar6);
	}
}
