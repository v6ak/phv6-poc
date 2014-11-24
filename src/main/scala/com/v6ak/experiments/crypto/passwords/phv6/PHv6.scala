package com.v6ak.experiments.crypto.passwords.phv6

import java.nio.charset.Charset

import scala.annotation.tailrec


object `package`{
	type Bytes = Array[Byte]
}

class PHv6(hash: Bytes => Bytes, encrypt: (Bytes, Bytes) => Bytes, totalRounds: Int){

	// TODO: Consider || vs | and optimizations. Using | might mitigate some timing attacks.

	if(totalRounds <= 0 ){
		throw new IllegalArgumentException("totalRounds has to be greater than zero")
	}

	@tailrec
	private def encryptMultipleTimes(data: Bytes, key: Bytes, n: Int): Bytes = n match {
		case 0 => data
		case _ if n < 0 => throw new IllegalArgumentException("n has to be greater than or equal to zero")
		case _ if n > 0 => encryptMultipleTimes(encrypt(data, key), key, n-1)
	}

	@tailrec
	private def checkInRounds(actual: Bytes, expected: Bytes, key: Bytes, n: Int, hasSucceeded: Boolean): Boolean = n match {
		case 0 => hasSucceeded
		case _ if n < 0 => throw new IllegalArgumentException("n has to be greater than or equal to zero")
		case _ if n > 0 =>
			val actualEncrypted = encrypt(actual, key)
			checkInRounds(
				actual = actualEncrypted,
				expected = expected,
				key = key,
				n = n-1,
				hasSucceeded = secureCompare(actualEncrypted, expected) || hasSucceeded // The order in || is intentional. This should prevent timing attacks.
			)
	}

	private def secureCompare(a: Bytes, b: Bytes): Boolean = a.length == b.length match {
		case true =>
			var different = false
			for(i <- 0 until a.length){
				different = (a(i) != b(i)) || different	// The order in || is intentional. It should prevent timing attacks.
			}
			!different
		case false => false	// This however indicates bad usage, possibly wrong hash function. We might consider throwing an exception there
	}

	def hide(password: String, charset: Charset): Bytes = hide(password.getBytes(Charset))

	def hide(password: Bytes): Bytes = {
		// TODO: Consider salt. (I know it can be handled separately, but it may be better to handle in the algorithm.)
		val passHash = hash(password)	// Ensures constant length of the output. The length thus does not reveal anything about the password
		encryptMultipleTimes(data = passHash, key = password, totalRounds)
	}

	def verify(givenPassword: String, passwordHash: Bytes, charset: Charset): Boolean = verify(
		givenPassword = givenPassword.getBytes(charset),
		passwordHash = passwordHash
	)
	
	def verify(givenPassword: Bytes, passwordHash: Bytes): Boolean = {
		val givenPasswordHash = hash(givenPassword)
		checkInRounds(
			actual = givenPasswordHash,
			expected = passwordHash,
			key = givenPassword,
			n = totalRounds,
			hasSucceeded = false
		)
	}

}