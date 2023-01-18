package de.fruxz.liscale.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

val scope = CoroutineScope(SupervisorJob())

fun task(process: suspend CoroutineScope.() -> Unit) = scope.launch(start = UNDISPATCHED, block = process)

fun <T> async(process: suspend CoroutineScope.() -> T) = scope.async(start = UNDISPATCHED, block = process)