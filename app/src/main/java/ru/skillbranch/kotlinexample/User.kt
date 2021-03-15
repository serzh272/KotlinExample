package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
        private val firstName: String,
        private val lastName: String?,
        email: String? = null,
        rawPhone: String? = null,
        meta: Map<String, Any>? = null
) {
    val userInfo: String
    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
                .joinToString(" ")
                .capitalize()
    private val initials: String
        get() = listOfNotNull(firstName, lastName)
                .map { it.first().toUpperCase() }
                .joinToString(" ")
    private var phone: String? = null
        set(value) {
            field = value?.replace("""[^+\d]""".toRegex(), "")
        }
    private var _login: String? = null
    var login: String
        set(value) {
            _login = value.toLowerCase()
        }
        get() = _login!!
    private var salt: String? = null
    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    constructor(
            firstName: String,
            lastName: String?,
            email: String?,
            password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary email constructor")
        passwordHash = encrypt(password)
    }

    constructor(
            firstName: String,
            lastName: String?,
            rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        sendAccessCodeToUser(rawPhone, updateRequestCode())
    }

    constructor(
            firstName: String,
            lastName: String?,
            email: String?,
            saltCsv: String,
            hash: String,
            rawPhone: String?
    ) : this(firstName, lastName, email = email, rawPhone = rawPhone, meta = mapOf("src" to "csv")) {
        passwordHash = hash
        salt = saltCsv
    }

    init {
        println("First init block? primary constructor was called")

        check(firstName.isNotBlank()) { "FirstName must not be blank" }
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) {
            "Email or phone must not be null or blank"
        }
        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash.also {
        println("Checking passwordHash is $passwordHash")
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        if (checkPassword(oldPassword)) {
            passwordHash = encrypt(newPassword)
            if (!accessCode.isNullOrEmpty()) accessCode = newPassword
            println("Password $oldPassword has been changed on new password $newPassword")
        } else throw IllegalArgumentException("The entered password does not match the current password")
    }

    fun updateRequestCode(): String {
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        println("Phone password Hash is $passwordHash")
        accessCode = code
        return code
    }

    private fun sendAccessCodeToUser(rawPhone: String, code: String) {
        println("...... sending access code: $code on $rawPhone")
    }

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun encrypt(password: String): String {
        if (salt.isNullOrEmpty()) {
            salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
        }
        println("Salt while encrypt: $salt")
        return salt.plus(password).md5()
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {
        fun makeUser(
                fullName: String,
                email: String? = null,
                password: String? = null,
                saltHash:String? = null,
                phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()


            return when {
                !phone.isNullOrBlank() && saltHash.isNullOrBlank() -> User(firstName, lastName, phone)
                !saltHash.isNullOrBlank() -> {
                    val l = saltHash?.split(":")
                    val salt = l.first()
                    val hash = l.last()
                    User(firstName, lastName, email, salt,hash, phone)
                }
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                        firstName,
                        lastName,
                        email,
                        password)
                else -> throw  IllegalArgumentException("Email or phone must not be null or blank")
            }
        }

        private fun String.fullNameToPair(): Pair<String, String?> =
                this.split(" ")
                        .filter { it.isNotBlank() }
                        .run {
                            when (size) {
                                1 -> first() to null
                                2 -> first() to last()
                                else -> throw IllegalArgumentException("FullName must contain only first name and last name, current split result: ${this@fullNameToPair}")
                            }
                        }
    }


}




