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
package tigase.halcyon.core.xmpp.modules.presence

import tigase.halcyon.core.Context
import tigase.halcyon.core.builder.ConfigurationDSLMarker
import tigase.halcyon.core.eventbus.Event
import tigase.halcyon.core.logger.LoggerFactory
import tigase.halcyon.core.modules.Criterion
import tigase.halcyon.core.modules.XmppModule
import tigase.halcyon.core.modules.XmppModuleProvider
import tigase.halcyon.core.requests.RequestBuilder
import tigase.halcyon.core.xml.Element
import tigase.halcyon.core.xmpp.BareJID
import tigase.halcyon.core.xmpp.JID
import tigase.halcyon.core.xmpp.getFromAttr
import tigase.halcyon.core.xmpp.stanzas.*

data class PresenceReceivedEvent(val jid: JID, val stanzaType: PresenceType?, val stanza: Presence) : Event(TYPE) {

	companion object {

		const val TYPE = "tigase.halcyon.core.xmpp.modules.presence.PresenceReceivedEvent"
	}
}

data class ContactChangeStatusEvent(
	val jid: BareJID, val status: String?, val presence: Presence, val lastReceivedPresence: Presence,
) : Event(TYPE) {

	companion object {

		const val TYPE = "tigase.halcyon.core.xmpp.modules.presence.ContactChangeStatusEvent"
	}
}

@ConfigurationDSLMarker
interface PresenceModuleConfig {

	var store: PresenceStore

}

class PresenceModule(override val context: Context) : XmppModule, PresenceModuleConfig {

	private val log = LoggerFactory.logger("tigase.halcyon.core.xmpp.modules.presence.PresenceModule")

	override val type = TYPE
	override val criteria = Criterion.name(Presence.NAME)
	override val features: Array<String>? = null

	override var store: PresenceStore = InMemoryPresenceStore()

	companion object : XmppModuleProvider<PresenceModule, PresenceModuleConfig> {

		override val TYPE = "PresenceModule"
		override fun instance(context: Context): PresenceModule = PresenceModule(context)

		override fun configure(module: PresenceModule, cfg: PresenceModuleConfig.() -> Unit) = module.cfg()
	}

	override fun initialize() {
	}

	override fun process(element: Element) {
		val presence: Presence = wrap(element)
		val fromJID = presence.getFromAttr()
		log.finer { "Presence received $presence" }
		if (fromJID == null) {
			return
		}

		if (presence.type == PresenceType.Unavailable) {
			store.removePresence(fromJID)
		} else if (presence.type == null || presence.type == PresenceType.Error) {
			store.setPresence(presence)
		}
		context.eventBus.fire(PresenceReceivedEvent(fromJID, presence.type, presence))

		if (presence.type == null || presence.type == PresenceType.Unavailable || presence.type == PresenceType.Error) {
			val bestPresence = getBestPresenceOf(fromJID.bareJID) ?: presence
			context.eventBus.fire(
				ContactChangeStatusEvent(
					fromJID.bareJID, bestPresence.status, bestPresence, presence
				)
			)
		}
	}

	fun sendInitialPresence() {
		val presence = presence { }
		context.writer.writeDirectly(presence)
	}

	fun sendPresence(
		jid: JID? = null, type: PresenceType? = null, show: Show? = null, status: String? = null,
	): RequestBuilder<Unit, Presence> {
		val presence = presence {
			if (jid != null) this.to = jid
			this.type = type
			if (show != null) this.show = show
			if (status != null) {
				this.status = status
			}
		}

		return context.request.presence(presence)
	}

	fun sendSubscriptionSet(jid: JID, presenceType: PresenceType): RequestBuilder<Unit, Presence> {
		return context.request.presence {
			to = jid
			type = presenceType
		}
	}

	fun getBestPresenceOf(jid: BareJID): Presence? {
		data class Envelope(val presence: Presence) {

			val comp: String by lazy { "${(500 - presence.priority)}:${100 + presence.typeAndShow().ordinal}" }
		}

		return store.getPresences(jid)
			.filter { presence -> presence.type == null }
			.map { presence -> Envelope(presence) }
			.minByOrNull { envelope -> envelope.comp }?.presence
	}

	fun getPresenceOf(jid: JID): Presence? {
		return store.getPresence(jid)
	}

	fun getResources(jid: BareJID): List<JID> {
		return store.getPresences(jid)
			.mapNotNull { p -> p.from }
	}

}