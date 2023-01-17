package de.fruxz.liscale.data.database

import de.fruxz.ascend.tool.timing.calendar.Calendar
import de.fruxz.liscale.data.config.Configuration
import de.fruxz.liscale.data.database.LicenseTable.sessions
import de.fruxz.liscale.data.database.LicenseTable.usages
import de.fruxz.liscale.data.domain.License
import de.fruxz.liscale.data.domain.License.Limit
import de.fruxz.liscale.data.domain.LicenseActivationRespond.DenyReason
import de.fruxz.liscale.data.domain.LicenseActivationRespond.DenyReason.*
import de.fruxz.liscale.extensions.async
import de.fruxz.liscale.extensions.smartTransaction
import de.fruxz.liscale.util.KeyUtil
import kotlinx.coroutines.Deferred
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object LicenseDataController {

	val database by lazy {
		with(Configuration) {
			Database.connect(databaseUrl, user = databaseUser, password = databaseKey)
		}
	}

	fun init() {
		::database.invoke() // initializing database value
		transaction(db = database) {
			SchemaUtils.create(UserTable, LicenseTable)
		}
	}

	fun listProducts(): List<String> = smartTransaction {
		return@smartTransaction LicenseTable
			.selectAll()
			.distinctBy { it[LicenseTable.product] }
			.map { it[LicenseTable.product] }
	}

	fun listLicenses(product: String): List<License> = smartTransaction {
		return@smartTransaction LicenseTable
			.select { LicenseTable.product eq product }
			.map { License(
				id = it[LicenseTable.id],
				product = it[LicenseTable.product],
				key = it[LicenseTable.key],
				status = it[LicenseTable.status],
				expiration = it[LicenseTable.expiration],
				limits = it[LicenseTable.limits],
				created = it[LicenseTable.created],
			) }
	}

	fun createLicense(
		product: String,
		expiration: Calendar? = null,
		limits: List<Limit>? = null,
	): License = smartTransaction {
		val key = KeyUtil.generateKey()

		LicenseTable.insert {
			it[this.product] = product
			it[this.key] = key
			it[this.expiration] = expiration
			it[this.limits] = limits
			it[this.created] = Calendar.now()
		}.let { result ->
			return@smartTransaction License(
				id = result[LicenseTable.id],
				product = result[LicenseTable.product],
				key = result[LicenseTable.key],
				status = result[LicenseTable.status],
				expiration = result[LicenseTable.expiration],
				limits = result[LicenseTable.limits],
				created = Calendar.now(), // TODO exception : result[LicenseTable.created]
			)
		}

	}

	fun getLicense(product: String, key: String): License? = smartTransaction {

		return@smartTransaction LicenseTable
			.select { (LicenseTable.product eq product) and (LicenseTable.key eq key) }
			.firstOrNull()
			?.let { result ->
				License(
					id = result[LicenseTable.id],
					product = result[LicenseTable.product],
					key = result[LicenseTable.key],
					status = result[LicenseTable.status],
					expiration = result[LicenseTable.expiration],
					limits = result[LicenseTable.limits],
					created = result[LicenseTable.created],
				)
		}

	}

	fun revokeLicense(product: String, key: String): Boolean = smartTransaction {
		if (getLicense(product, key)?.status != License.Status.ACTIVE) return@smartTransaction false

		LicenseTable.update({ LicenseTable.product eq product and (LicenseTable.key eq key) }) {
			it[status] = License.Status.REVOKED
		}.let { result ->
			return@smartTransaction result > 0
		}

	}

	fun deleteLicense(product: String, key: String): Boolean = smartTransaction {

		LicenseTable.deleteWhere { LicenseTable.product eq product and (LicenseTable.key eq key) }.let { result ->
			return@smartTransaction result > 0
		}

	}

	fun licenseDenyStatus(product: String, key: String): Deferred<DenyReason?> = async {
		val license = getLicense(product, key) ?: return@async NOT_FOUND
		val limits = license.limits

		return@async licenseDenyStatus(license, limits).await()
	}

	private fun licenseDenyStatus(license: License, limits: List<Limit>?): Deferred<DenyReason?> = async {

		if (license.expiration?.inPast == true) return@async EXPIRED
		if (license.status != License.Status.ACTIVE) return@async ACCESS_REVOKED
		limits?.firstOrNull { it.isAchieved() }?.let { return@async it.denyRespond }

		return@async null
	}

	fun activateLicense(product: String, key: String): Deferred<DenyReason?> = smartTransaction {
		async {
			val license = getLicense(product, key) ?: return@async NOT_FOUND
			val limits = license.limits

			// checks

			licenseDenyStatus(license, limits).await()?.let { return@async it }

			// enable license

			val (sessions, usages) = LicenseTable.select { LicenseTable.product eq product and (LicenseTable.key eq key) }
				.firstOrNull()
				?.let { result ->
					result[sessions] to result[usages]
				} ?: return@async NOT_FOUND

			LicenseTable.update({ LicenseTable.product eq product and (LicenseTable.key eq key) }) {
				it[LicenseTable.sessions] = sessions + 1
				it[LicenseTable.usages] = usages + 1
			}.let { result ->
				return@async if (result > 0) null else NOT_FOUND
			}

		}
	}

	fun disableLicense(product: String, key: String): Boolean = smartTransaction {
		val sessions = LicenseTable
			.select { LicenseTable.product eq product and (LicenseTable.key eq key) }
			.firstOrNull()
			?.getOrNull(LicenseTable.sessions) ?: return@smartTransaction false

		if (sessions <= 0) return@smartTransaction false

		LicenseTable.update({ LicenseTable.product eq product and (LicenseTable.key eq key) }) {
			it[LicenseTable.sessions] = sessions - 1
		}.let { result ->
			return@smartTransaction result > 0
		}

	}

}