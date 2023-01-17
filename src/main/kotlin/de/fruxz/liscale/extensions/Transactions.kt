package de.fruxz.liscale.extensions

import de.fruxz.liscale.data.database.LicenseDataController
import de.fruxz.liscale.data.database.UserDataController
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

fun <T> smartTransaction(process: Transaction.() -> T): T {
	LicenseDataController.init()

	return transaction(db = LicenseDataController.database, statement = process)
}