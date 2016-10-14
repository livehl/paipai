package common

import java.security.{Key, SecureRandom}
import javax.crypto.{Cipher, KeyGenerator}
import javax.crypto.spec.SecretKeySpec

import common.Tool._

/**
 * AES加解密工具类
 */
object AESCoder {

  /**
   * 生成密钥
   *
   * @param key   二进制密钥
   * @return 密钥
   */
  private def toKey(key: Array[Byte]): Key = {
    val random = SecureRandom.getInstance("SHA1PRNG")
    random.setSeed(key)
    val kgen = KeyGenerator.getInstance(KEY_ALGORITHM)
    kgen.init(128, random)
    val secretKey = kgen.generateKey()
    val enCodeFormat = secretKey.getEncoded()
    return new SecretKeySpec(enCodeFormat, KEY_ALGORITHM)
  }

  def toKey(key: String): Key = {
    return toKey(key.getBytes("utf-8"))
  }

  /**
   * 加密
   *
   * @param data  待加密数据
   * @param key   二进制密钥
   * @return byte[]   加密数据
   * @throws Exception
   */
  def encrypt(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val k: Key = toKey(key)
    return encrypt(data, k)
  }

  /**
   * 加密，返回hex字符串
   * @param data
   * @param key
   * @return
   */
  def encrypt(data: String, key: String): String = {
    return bytes2hex(encrypt(data.getBytes("utf-8"), toKey(key.getBytes("utf-8"))))
  }

  /**
   * 加密数据
   * @param data
   * @param key
   * @return
   */
  def encrypt(data: String, key: Key): String = {
    return bytes2hex(encrypt(data.getBytes("utf-8"), key))
  }


  /**
   * 加密
   *
   * @param data  待加密数据
   * @param key   密钥
   * @return byte[]   加密数据
   * @throws Exception
   */
  private def encrypt(data: Array[Byte], key: Key): Array[Byte] = {
    val cipher: Cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    return cipher.doFinal(data)
  }

  /**
   * 解密
   *
   * @param data  待解密数据
   * @param key   二进制密钥
   * @return byte[]   解密数据
   * @throws Exception
   */
  def decrypt(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val k: Key = toKey(key)
    return decrypt(data, k)
  }

  /**
   * 解密hex字符串为原始数据
   * @param data
   * @param key
   * @return
   */
  def decrypt(data: String, key: String): String = {
    return new String(decrypt(hex2bytes(data), toKey(key.getBytes("utf-8"))),"utf-8")
  }

  /**
   * 解密数据
   * @param data
   * @param key
   * @return
   */
  def decrypt(data: String, key: Key): String = {
    return new String(decrypt(hex2bytes(data), key),"utf-8")
  }

  /**
   * 解密
   *
   * @param data  待解密数据
   * @param key   密钥
   * @return byte[]   解密数据
   * @throws Exception
   */
  private def decrypt(data: Array[Byte], key: Key): Array[Byte] = {
    val cipher: Cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, key)
    return cipher.doFinal(data)
  }

  /**
   * 密钥算法
   */
  private val KEY_ALGORITHM: String = "AES"
  private val DEFAULT_CIPHER_ALGORITHM: String = "AES/ECB/PKCS5Padding"
}