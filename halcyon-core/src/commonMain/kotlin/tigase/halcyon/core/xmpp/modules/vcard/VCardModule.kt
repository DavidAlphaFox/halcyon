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
package tigase.halcyon.core.xmpp.modules.vcard

import tigase.halcyon.core.Context
import tigase.halcyon.core.eventbus.Event
import tigase.halcyon.core.modules.Criteria
import tigase.halcyon.core.modules.XmppModule
import tigase.halcyon.core.requests.RequestBuilder

import tigase.halcyon.core.xml.Element
import tigase.halcyon.core.xmpp.BareJID
import tigase.halcyon.core.xmpp.ErrorCondition
import tigase.halcyon.core.xmpp.XMPPException
import tigase.halcyon.core.xmpp.modules.BindModule
import tigase.halcyon.core.xmpp.modules.pubsub.PubSubEventReceivedEvent
import tigase.halcyon.core.xmpp.stanzas.IQ
import tigase.halcyon.core.xmpp.stanzas.IQType
import tigase.halcyon.core.xmpp.stanzas.iq
import tigase.halcyon.core.xmpp.toJID

data class VCardUpdatedEvent(val jid: BareJID, val vcard: VCard?) : Event(TYPE) { companion object {

	const val TYPE = "tigase.halcyon.core.xmpp.modules.vcard.VCardUpdatedEvent"
}
}

class VCardModule(override val context: Context) : XmppModule {

	companion object {

		const val XMLNS = "urn:ietf:params:xml:ns:vcard-4.0"
		const val NODE = "urn:xmpp:vcard4"
		const val TYPE = XMLNS
	}

	override val criteria: Criteria? = null
	override val features: Array<String> = arrayOf(XMLNS, "$NODE+notify")
	override val type = TYPE

	/**
	 * If `true`, vCard will be retrieved after receiving information about vCard update.
	 */
	var autoRetrieve: Boolean = false

	override fun initialize() {
		context.eventBus.register(PubSubEventReceivedEvent.TYPE, this@VCardModule::processEvent)
	}

	override fun process(element: Element) = throw XMPPException(ErrorCondition.FeatureNotImplemented)

	/**
	 * Prepares request to retrieve vcard.
	 * @param jid address of entity what VCard have to be retrieved.
	 * @return builder of request returning [VCard] as result.
	 */
	fun retrieveVCard(jid: BareJID): RequestBuilder<VCard, IQ> {
		val iq = iq {
			type = IQType.Get
			to = jid.toJID()
			"vcard"{
				xmlns = XMLNS
			}
		}
		return context.request.iq(iq).map(this@VCardModule::parseResponse)
	}

	/**
	 * Prepares request to publish own VCard.
	 *
	 * @param vcard VCard to be published.
	 */
	fun publish(vcard: VCard): RequestBuilder<Unit, IQ> {
		val ownJid = context.modules.getModuleOrNull<BindModule>(BindModule.TYPE)?.boundJID?.bareJID
		val iq = iq {
			type = IQType.Set
			ownJid?.let {
				to = it.toString().toJID()
			}
			"vcard"{
				xmlns = XMLNS
			}
		}
		return context.request.iq(iq).map { Unit }
	}

	private fun processEvent(event: PubSubEventReceivedEvent) {
		if (event.nodeName != NODE) return
		val jid = event.pubSubJID ?: return

		if (autoRetrieve) {
			retrieveVCard(jid.bareJID).response {
				if (it.isSuccess) {
					context.eventBus.fire(VCardUpdatedEvent(jid.bareJID, it.getOrNull()))
				}
			}.send()
		} else {
			context.eventBus.fire(VCardUpdatedEvent(jid.bareJID, null))
		}
	}

	private fun parseResponse(iq: Element): VCard {
		val vCard = iq.getChildrenNS("vcard", XMLNS) ?: throw  XMPPException(ErrorCondition.BadRequest)
		return VCard(vCard)
	}

}