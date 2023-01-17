package de.fruxz.liscale.util

import de.fruxz.ascend.extension.data.RandomTagType.MIXED_CASE
import de.fruxz.ascend.extension.data.buildRandomTag

object KeyUtil {

	fun generateKey(): String = buildList {
		repeat(4) {
			add(buildRandomTag(hashtag = false, tagType = MIXED_CASE))
		}
	}.joinToString("-")

}