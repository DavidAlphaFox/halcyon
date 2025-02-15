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
package tigase.halcyon.core.modules

import tigase.halcyon.core.ClearedEvent
import tigase.halcyon.core.Context
import tigase.halcyon.core.Scope
import tigase.halcyon.core.eventbus.EventBus
import tigase.halcyon.core.logger.LoggerFactory
import tigase.halcyon.core.xml.Element
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface XmppModuleProvider<out M : XmppModule, Configuration : Any> {

	val TYPE: String

	fun instance(context: Context): M

	fun configure(module: @UnsafeVariance M, cfg: Configuration.() -> Unit)

	fun requiredModules(): List<XmppModuleProvider<XmppModule, out Any>> = emptyList()

}

interface XmppModule {

	val type: String

	val context: Context

	val criteria: Criteria?

	val features: Array<String>?

	fun initialize()

	fun process(element: Element)

	fun <T> propertySimple(scope: Scope, initialValue: T): ReadWriteProperty<Any?, T> =
		context.propertySimple(scope, initialValue)

	fun <T> property(scope: Scope, initialValueFactory: (() -> T)): ReadWriteProperty<Any?, T> =
		context.property(scope, initialValueFactory)

}

class ClearableValue<T>(
	eventBus: EventBus, private val scope: Scope, private val initialValueFactory: (() -> T),
) : ReadWriteProperty<Any?, T> {

	private val log = LoggerFactory.logger("tigase.halcyon.core.modules.ClearableValue")

	private var value = initialValueFactory.invoke()

	init {
		eventBus.register(ClearedEvent.TYPE, this::clear)
		log.finest { "Registered cleaner scope=$scope; initialValueFactory=$initialValueFactory" }
	}

	private fun clear(event: ClearedEvent) {
		log.finest("ClearEvent #event")
		if (event.scopes.contains(scope)) {
			this.value = initialValueFactory.invoke()
			log.fine { "Restoring initial value. Scope=$scope; value=$value" }
		}
	}

	override fun getValue(thisRef: Any?, property: KProperty<*>): T {
		return value
	}

	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
		this.value = value
	}

}

fun <T> Context.propertySimple(scope: Scope, initialValue: T): ReadWriteProperty<Any?, T> =
	ClearableValue(eventBus, scope) { initialValue }

fun <T> Context.property(scope: Scope, initialValueFactory: (() -> T)): ReadWriteProperty<Any?, T> =
	ClearableValue(eventBus, scope, initialValueFactory)
