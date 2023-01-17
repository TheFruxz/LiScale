package de.fruxz.liscale.data.database

import de.fruxz.ascend.extension.security.sha512
import de.fruxz.liscale.data.domain.User
import de.fruxz.liscale.extensions.smartTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

object UserDataController {

	fun listUsers(): List<String> = smartTransaction {
		UserTable.selectAll().map { it[UserTable.username] }
	}

	fun validateUser(username: String, password: String): Boolean = smartTransaction {
		val user = UserTable.select { UserTable.username eq username }.firstOrNull() ?: return@smartTransaction false
		return@smartTransaction user[UserTable.password] == sha512(password)
	}

	fun getUser(username: String): User? = smartTransaction {
		UserTable.select { UserTable.username eq username }.firstOrNull()?.let {
			User(
				id = it[UserTable.id],
				username = it[UserTable.username],
				assignedProducts = it[UserTable.assignedProducts],
			)
		}
	}

	fun getUser(username: String, password: String): User? = smartTransaction {
		UserTable.select { UserTable.username eq username }.firstOrNull()?.let {
			if (it[UserTable.password] == sha512(password)) {
				User(
					id = it[UserTable.id],
					username = it[UserTable.username],
					assignedProducts = it[UserTable.assignedProducts],
				)
			} else null
		}
	}

	fun listUserProducts(username: String): List<String>? =
		getUser(username)?.assignedProducts

	fun setUserProducts(username: String, assignedProducts: List<String>): Boolean = smartTransaction {
		UserTable.update({ UserTable.username eq username }) {
			it[UserTable.assignedProducts] = assignedProducts
		}.let {
			return@smartTransaction it > 0
		}
	}

	fun deleteUser(username: String): Boolean = smartTransaction {
		UserTable.deleteWhere { UserTable.username eq username }.let {
			return@smartTransaction it > 0
		}
	}

	fun createUser(username: String, password: String, assignedProducts: List<String>): Boolean = smartTransaction {
		if (getUser(username) != null) return@smartTransaction false

		UserTable.insert {
			it[UserTable.username] = username
			it[UserTable.password] = sha512(password)
			it[UserTable.assignedProducts] = assignedProducts
		}

		return@smartTransaction true
	}

	fun updateUserPassword(username: String, newPassword: String): Boolean = smartTransaction {
		UserTable.update({ UserTable.username eq username }) {
			it[UserTable.password] = sha512(newPassword)
		}.let {
			return@smartTransaction it > 0
		}
	}

}