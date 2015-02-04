package org.hive2hive.core.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.hive2hive.core.model.BaseNetworkContent;
import org.hive2hive.core.model.versioned.EncryptedNetworkContent;
import org.hive2hive.core.model.versioned.HybridEncryptedContent;

public interface IH2HEncryption {

	/**
	 * Symmetrically encrypts content inheriting from {@link BaseNetworkContent} by means of the AES
	 * algorithm.
	 * The content first gets serialized, then encrypted.
	 * 
	 * @param content the content to be encrypted. Can be of any type that extends {@link BaseNetworkContent}.
	 * @param aesKey The symmetric key with which the content will be encrypted.
	 * @return EncryptedContent which contains the encrypted byte[] content as well as the AES initialization
	 *         vector (IV).
	 * @throws InvalidCipherTextException
	 * @throws IllegalStateException
	 * @throws DataLengthException
	 * @throws IOException
	 */
	EncryptedNetworkContent encryptAES(BaseNetworkContent content, SecretKey aesKey) throws InvalidCipherTextException,
			IOException;

	/**
	 * Symmetrically decrypts a prior content inheriting from {@link BaseNetworkContent} by means of the AES
	 * algorithm. The content gets deserialized after the decryption.
	 * 
	 * @param content The EncryptedContent to be decrypted.
	 * @param aesKey The symmetric key with which the content will be decrypted.
	 * @return decrypted object
	 * @throws InvalidCipherTextException
	 * @throws IllegalStateException
	 * @throws DataLengthException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	BaseNetworkContent decryptAES(EncryptedNetworkContent content, SecretKey aesKey) throws InvalidCipherTextException,
			ClassNotFoundException, IOException;

	/**
	 * Asymmetrically encrypts content inheriting from {@link BaseNetworkContent}. A default key length will
	 * be used.
	 * 
	 * @param content the content to be encrypted.
	 * @param publicKey The asymmetric public key with which the content will be encrypted
	 * @return the encrypted content
	 * @throws DataLengthException
	 * @throws InvalidKeyException
	 * @throws IllegalStateException
	 * @throws InvalidCipherTextException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IOException
	 */
	HybridEncryptedContent encryptHybrid(BaseNetworkContent content, PublicKey publicKey) throws InvalidKeyException,
			InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, IOException;

	/**
	 * Asymmetrically encrypts any content that is already serialized. A default key length will be
	 * used.
	 * 
	 * @param content the content to be encrypted.
	 * @param publicKey The asymmetric public key with which the content will be encrypted
	 * @return the encrypted content
	 * @throws DataLengthException
	 * @throws InvalidKeyException
	 * @throws IllegalStateException
	 * @throws InvalidCipherTextException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	HybridEncryptedContent encryptHybrid(byte[] content, PublicKey publicKey) throws InvalidKeyException,
			InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException;

	/**
	 * Asymmetrically decrypts a prior content inheriting from {@link BaseNetworkContent}.
	 * 
	 * @param content the encrypted content to be decrypted
	 * @param privateKey the asymmetric private key that matches the public key at encryption
	 * @return decrypted object
	 * @throws InvalidKeyException
	 * @throws DataLengthException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IllegalStateException
	 * @throws InvalidCipherTextException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	BaseNetworkContent decryptHybrid(HybridEncryptedContent content, PrivateKey privateKey) throws InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, InvalidCipherTextException, ClassNotFoundException, IOException;

	/**
	 * Asymmetrically decrypts any content
	 * 
	 * @param content the encrypted content to be decrypted
	 * @param privateKey the asymmetric private key that matches the public key at encryption
	 * @return decrypted object in the raw format
	 * @throws InvalidKeyException
	 * @throws DataLengthException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IllegalStateException
	 * @throws InvalidCipherTextException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	byte[] decryptHybridRaw(HybridEncryptedContent content, PrivateKey privateKey) throws InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, InvalidCipherTextException, ClassNotFoundException, IOException;

}