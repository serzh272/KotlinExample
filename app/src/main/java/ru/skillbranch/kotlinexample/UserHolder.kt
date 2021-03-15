package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
            fullName: String,
            email: String,
            password: String
    ): User {
        val usr = User.makeUser(fullName, email = email, password = password)
                .also { user ->
                    if (map.containsKey(user.login)) throw IllegalArgumentException("A user with this email already exists")
                    map[user.login] = user
                }
        return usr
    }

    fun loginUser(login: String, password: String): String? =
            map[login.trim()]?.let {
                if (it.checkPassword(password)) it.userInfo
                else null
            }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

    fun registerUserByPhone(fullName: String, phone: String): User {

        val usr = User.makeUser(fullName, phone = phone)
                .also { user ->
                    if (map.containsKey(phone)) throw IllegalArgumentException("A user with this phone already exists")
                    if (!user.login.startsWith('+') || user.login.filter { it.isDigit() }.length != 11) throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
                    map[phone] = user
                }
        return usr
    }

    fun requestAccessCode(phone: String) {
        map[phone]?.updateRequestCode()
    }

    fun importUsers(list: List<String>): List<User> {
        var result = mutableListOf<User>()
        for (str in list) {
            val strUsr = str.split(";").map { if (it == "") null else it }
            val user = User.makeUser(
                    fullName = strUsr[0]!!,
                    email = strUsr[1],
                    saltHash = strUsr[2],
                    phone = strUsr[3]
            ).also { user -> map[user.login] = user }
            result.add(user)
        }
        return result
    }

}
