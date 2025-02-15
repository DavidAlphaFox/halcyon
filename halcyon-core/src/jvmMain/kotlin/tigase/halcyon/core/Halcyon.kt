/*
 * halcyon-core
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
package tigase.halcyon.core

import tigase.halcyon.core.builder.ConfigurationBuilder
import tigase.halcyon.core.connector.AbstractConnector
import tigase.halcyon.core.connector.socket.SocketConnector
import tigase.halcyon.core.eventbus.Event
import tigase.halcyon.core.eventbus.EventBus
import tigase.halcyon.core.eventbus.EventHandler
import tigase.halcyon.core.exceptions.AuthenticationException
import tigase.halcyon.core.exceptions.HalcyonException
import tigase.halcyon.core.logger.LoggerFactory
import tigase.halcyon.core.xmpp.modules.auth.SASLEvent
import java.util.*

actual class Halcyon actual constructor(configuration: ConfigurationBuilder) : AbstractHalcyon(configuration) {

	private val log = LoggerFactory.logger("tigase.halcyon.core.Halcyon")

	override fun createConnector(): AbstractConnector {
		return SocketConnector(this)
	}

	override fun reconnect(immediately: Boolean) {
		log.finer { "Called reconnect. immediately=$immediately" }
		if (!immediately) Thread.sleep(3000)
		state = State.Connecting
		startConnector()
	}

	val timer = Timer("timer", true)

	private val lock = Object()

	private lateinit var tickTask: TimerTask

	init {
		eventBus.mode = EventBus.Mode.ThreadPerHandler
//		this.config.connectorConfig = SocketConnectorConfig()
	}

	override fun onConnecting() {
		super.onConnecting()
		tickTask = object : TimerTask() {
			override fun run() {
				tick()
			}
		}
		timer.scheduleAtFixedRate(tickTask, 2_000, 2_000)
	}

	override fun onDisconnecting() {
		tickTask.cancel()
		super.onDisconnecting()
	}

	fun waitForAllResponses() {
		while (requestsManager.getWaitingRequestsSize() > 0) {
			println(requestsManager.getRequestsIDs())

			synchronized(lock) {
				lock.wait(100)
			}
		}
	}

	fun connectAndWait() {
		var exceptionToThrow: Throwable? = null
		val handler = object : EventHandler<Event> {
			override fun onEvent(event: Event) {
				if (event is SASLEvent.SASLError) {
					exceptionToThrow = AuthenticationException(event.error, event.description ?: "Authentication error")
				} else if (event is HalcyonStateChangeEvent) {
					if (event.newState == State.Connected || event.newState == State.Stopped) {
						synchronized(lock) {
							lock.notify()
						}
					}
				}
			}
		}
		try {
			eventBus.register(handler = handler)
			super.connect()
			synchronized(lock) {
				lock.wait(30000)
			}
			exceptionToThrow?.let { throw it }
			if (state != State.Connected && state != State.Stopped) {
				throw HalcyonException("Cannot connect to XMPP server.")
			}
		} finally {
			eventBus.unregister(handler)
		}
	}
}