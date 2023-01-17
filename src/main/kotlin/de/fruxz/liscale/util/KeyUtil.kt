package de.fruxz.liscale.util

import de.fruxz.ascend.extension.data.RandomTagType.MIXED_CASE
import de.fruxz.ascend.extension.data.buildRandomTag
import de.fruxz.liscale.data.config.Configuration

object KeyUtil {

	fun generateKey(): String {
		var state = Configuration.keyFormat

		while (state.contains("%")) {
			state = state.replaceFirst("%", buildRandomTag(hashtag = false, tagType = MIXED_CASE, size = Configuration.keyBlockLength))
		}

		return state
	}

}