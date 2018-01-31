package org.tigase.jaxmpp.core.xml.parser

import org.tigase.jaxmpp.core.xml.Element
import org.tigase.jaxmpp.core.xml.ElementBuilder
import java.lang.StringBuilder
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class XMPPDomHandler(val onNextElement: (Element) -> Unit, val onStreamStarted: (Map<String, String>) -> Unit,
					 val onStreamClosed: () -> Unit, val onParseError: (String) -> Unit) : SimpleHandler {

	companion object {
		private val ELEM_STREAM_STREAM = "stream:stream"

	}

	private val log = Logger.getLogger(XMPPDomHandler::class.java.name)

	private val namespaces = TreeMap<String, String>()

	private var parserState: Any? = null

	private var elementBuilder: ElementBuilder? = null

	override fun endElement(name: StringBuilder?): Boolean {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("End element name: " + name)
		}

		val tmp_name = name.toString()

		if (tmp_name == ELEM_STREAM_STREAM) {
			onStreamClosed.invoke()
			return true
		}

		val elemName = tmp_name.substringAfter(":")

		if (elementBuilder != null && elementBuilder!!.onTop && elementBuilder!!.currentElement.name == elemName) {
			val element = elementBuilder!!.element
			elementBuilder = null
			onNextElement.invoke(element)
			return true
		} else if (elementBuilder != null && elementBuilder!!.currentElement.name == elemName) {
			elementBuilder!!.up()
			return true
		}

		return false;
	}

	override fun startElement(name: StringBuilder, attr_names: Array<StringBuilder?>?,
							  attr_values: Array<StringBuilder>?) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Start element name: " + name)
			log.finest("Element attributes names: " + Arrays.toString(attr_names))
			log.finest("Element attributes values: " + Arrays.toString(attr_values))
		}

		// Look for 'xmlns:' declarations:
		if (attr_names != null) {
			for (i in attr_names.indices) {

				// Exit the loop as soon as we reach end of attributes set
				if (attr_names[i] == null) {
					break
				}

				if (attr_names[i].toString().startsWith("xmlns:")) {

					// TODO should use a StringCache instead of intern() to
					// avoid potential
					// DOS by exhausting permgen
					namespaces.put(attr_names[i]!!.substring("xmlns:".length, attr_names[i]!!.length).intern(),
								   attr_values!![i].toString())

					if (log.isLoggable(Level.FINEST)) {
						log.finest("Namespace found: " + attr_values[i].toString())
					}
				} // end of if (att_name.startsWith("xmlns:"))
			} // end of for (String att_name : attnames)
		} // end of if (attr_names != null)

		var tmp_name = name.toString()

		if (tmp_name == ELEM_STREAM_STREAM) {
			val attribs = HashMap<String, String>()

			if (attr_names != null) {
				for (i in attr_names.indices) {
					if (attr_names[i] != null && attr_values!![i] != null) {
						attribs[attr_names[i].toString()] = attr_values[i].toString()
					} else {
						break
					} // end of else
				} // end of for (int i = 0; i < attr_names.length; i++)
			} // end of if (attr_name != null)

			onStreamStarted(attribs)

			return
		} // end of if (tmp_name.equals(ELEM_STREAM_STREAM))

		var new_xmlns: String? = null
		var prefix: String? = null
		var tmp_name_prefix: String? = null
		val idx = tmp_name.indexOf(':')

		if (idx > 0) {
			tmp_name_prefix = tmp_name.substring(0, idx)

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Found prefixed element name, prefix: " + tmp_name_prefix)
			}
		}

		if (tmp_name_prefix != null) {
			for (pref in namespaces.keys) {
				if (tmp_name_prefix == pref) {
					new_xmlns = namespaces.get(pref)
					tmp_name = tmp_name.substring(pref.length + 1, tmp_name.length)
					prefix = pref

					if (log.isLoggable(Level.FINEST)) {
						log.finest("new_xmlns = " + new_xmlns!!)
					}
				} // end of if (tmp_name.startsWith(xmlns))
			} // end of for (String xmlns: namespaces.keys())
		}

		val attribs = mutableMapOf<String, String>()
		if (attr_names != null) {
			for (i in 0 until attr_names.size) {
				val k = attr_names[i]
				val v = attr_values!![i]
				if (k != null && v != null) attribs[k.toString()] = v.toString()
			}
		}

		if (elementBuilder != null) {
			elementBuilder!!.child(tmp_name)
		} else {
			elementBuilder = ElementBuilder.Companion.create(tmp_name)
		}

		if (new_xmlns != null) {
			elementBuilder!!.xmlns(new_xmlns)
			attribs.remove("xmlns:" + prefix!!)

			if (log.isLoggable(Level.FINEST)) {
				log.finest("new_xmlns assigned: " + elementBuilder!!.currentElement.getAsString())
			}
		}

		elementBuilder!!.attributes(attribs)

	}

	override fun restoreParserState(): Any? = parserState

	override fun elementCData(cdata: StringBuilder?) {
		elementBuilder!!.value(cdata.toString())
	}

	override fun saveParserState(state: Any?) {
		this.parserState = state
	}

	override fun otherXML(other: StringBuilder?) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Other XML content: " + other)
		}
	}

	override fun error(errorMessage: String?) {
		log.warning("XML content parse error.")

		if (log.isLoggable(Level.FINE)) {
			log.fine(errorMessage)
		}

		onParseError.invoke(errorMessage ?: "")
	}
}