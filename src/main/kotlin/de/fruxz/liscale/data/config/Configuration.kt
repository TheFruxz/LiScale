package de.fruxz.liscale.data.config

import de.fruxz.ascend.extension.getHomePath
import de.fruxz.ascend.tool.delegate.property
import kotlin.io.path.div

object Configuration {

	private val config = getHomePath() / "LiScale.config.json"

	val keyFormat by property(config, "key-format") { "%-%-%-%-%" }

	val keyBlockLength by property(config, "key-block-length") { 5 }

	val databaseUrl by property(config, "db_url") { "jdbc:sqlite:LiScale.db" }

	val databaseUser by property(config, "db_user") { "" }

	val databaseKey by property(config, "db_key") { "" }

}