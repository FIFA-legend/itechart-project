package com.itechart.project.authentication

import cats.effect.Sync
import cats.syntax.all._
import com.itechart.project.configuration.ConfigurationTypes.PasswordSalt
import com.itechart.project.domain.user.{EncryptedPassword, Password}

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.{Cipher, SecretKeyFactory}
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}

trait Crypto {
  def encrypt(value: Password):          EncryptedPassword
  def decrypt(value: EncryptedPassword): Password
}

object Crypto {
  def of[F[_]: Sync](passwordSalt: PasswordSalt): F[Crypto] = Sync[F]
    .delay {
      val random = new SecureRandom(passwordSalt.secret.value.getBytes("UTF-8"))
      val bytes  = new Array[Byte](16)
      random.nextBytes(bytes)
      val vector        = new IvParameterSpec(bytes)
      val salt          = passwordSalt.secret.value.getBytes("UTF-8")
      val pbeKey        = new PBEKeySpec("password".toCharArray, salt, 65536, 256)
      val factory       = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
      val encodedBytes  = factory.generateSecret(pbeKey).getEncoded
      val secretKey     = new SecretKeySpec(encodedBytes, "AES")
      val encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, vector)
      val decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, vector)
      (encryptCipher, decryptCipher)
    }
    .map { case (encryptCipher, decryptCipher) =>
      new Crypto {
        override def encrypt(password: Password): EncryptedPassword = {
          val base64 = Base64.getEncoder
          val bytes  = password.value.getBytes("UTF-8")
          val result = new String(base64.encode(encryptCipher.doFinal(bytes)), "UTF-8")
          EncryptedPassword(result)
        }

        override def decrypt(password: EncryptedPassword): Password = {
          val base64 = Base64.getDecoder
          val bytes  = base64.decode(password.value.getBytes("UTF-8"))
          val result = new String(decryptCipher.doFinal(bytes), "UTF-8")
          Password(result)
        }
      }
    }
}
