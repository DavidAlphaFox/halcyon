/*
 * Tigase Halcyon XMPP Library
 * Copyright (C) 2018 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.halcyon.core.logger

import java.util.logging.LogRecord

@Suppress("NOTHING_TO_INLINE")
actual class Logger actual constructor(name: String, val enabled: Boolean) {

	private val log = java.util.logging.Logger.getLogger(name)

	private inline fun cnv(level: Level): java.util.logging.Level = when (level) {
		Level.OFF -> java.util.logging.Level.OFF
		Level.SEVERE -> java.util.logging.Level.SEVERE
		Level.WARNING -> java.util.logging.Level.WARNING
		Level.INFO -> java.util.logging.Level.INFO
		Level.CONFIG -> java.util.logging.Level.CONFIG
		Level.FINE -> java.util.logging.Level.FINE
		Level.FINER -> java.util.logging.Level.FINER
		Level.FINEST -> java.util.logging.Level.FINEST
		Level.ALL -> java.util.logging.Level.ALL
	}

	actual fun isLoggable(level: Level): Boolean {
		return log.isLoggable(cnv(level))
	}

	private inline fun doLog(level: Level, msg: String, caught: Throwable?) {
		if (!enabled) return
		val lr = LogRecord(cnv(level), msg)
		if (caught != null) lr.thrown = caught

		fillCaller(lr)

		log.log(lr)
	}

	private fun fillCaller(lr: LogRecord) {
		val trace = Throwable()
		val list = trace.stackTrace

		list.find { stackTraceElement ->
			!stackTraceElement.className.startsWith(
				"tigase.halcyon.core.logger."
			)
		}.let { stackTraceElement ->
			if (stackTraceElement != null) {
				lr.sourceClassName = stackTraceElement.className
				lr.sourceMethodName = stackTraceElement.methodName
			}
		}
	}

	actual fun log(level: Level, msg: String) {
		doLog(level, msg, null)
	}

	actual fun log(level: Level, msg: String, caught: Throwable) {
		doLog(level, msg, caught)
	}

	actual fun fine(msg: String) {
		doLog(Level.FINE, msg, null)
	}

	actual fun finer(msg: String) {
		doLog(Level.FINER, msg, null)
	}

	actual fun finest(msg: String) {
		doLog(Level.FINEST, msg, null)
	}

	actual fun config(msg: String) {
		doLog(Level.CONFIG, msg, null)
	}

	actual fun info(msg: String) {
		doLog(Level.INFO, msg, null)
	}

	actual fun warning(msg: String) {
		doLog(Level.WARNING, msg, null)
	}

	actual fun severe(msg: String) {
		doLog(Level.SEVERE, msg, null)
	}

}