MAMModule
----------

This module implements `XEP-0313: Message Archive Management <https://xmpp.org/extensions/xep-0313.html>`__.

Events
^^^^^^^^^^^

``MAMMessageEvent``
~~~~~~~~~~~~~~~~~~~~~~~

This event is fired, when each result for ``query()`` method is received.

This event contains fields:

-  ``resultStanza`` - whole received Message stanza (response for query),

-  ``queryId`` - query identifier,

-  ``id`` - result identifier,

-  ``forwardedStanza`` - result for query containing stanza from archive and original receiving timestamp

Methods
^^^^^^^^

``query(to: BareJID? = null, node: String? = null, rsm: RSM.Query? = null, with: String? = null, start: Long? = null, end: Long? = null)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Main method to retrieve chat history from archive.

Arguments:

-  ``to`` - JID of MAM component. If null, then default MAM component of user server is used,

-  ``node`` - name of node,

-  ``rsm`` - Result Set Management object,

-  ``with`` - name (JID) of interlocutor

-  ``start``, ``end`` - timestamps to filter messages by receive date

All of above argoments may be ``null``.

``retrievePreferences()``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Retrieves MAM preferences.

In response, you will get ``Preferences`` object containing:

-  ``default`` - default behaviour of message archiving: ``Always``, ``Never``, ``Roster``.

-  ``always`` - collection of BareJIDs with whom conversations will always be archived.

-  ``never`` - collection of BareJIDs with whom conversations will never be archived.

``updatePreferences(preferences: Preferences)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Updates MAM preferences.

Usage
^^^^^

When client establish connection to server, it should ask server for all messages exchanged with other clients connected to the same account.

It can be done by asking server for all messages since, last received message:

.. code:: kotlin

   val mamModule = halcyon.getModule<MAMModule>(MAMModule.TYPE)
   fun ask(q: RSM.Query? = null) {
       mamModule.query(
           with = "someone@tigase.org",
           start = lastReceivedMessageTimestatmp,
           rsm = q
       ).response { res ->
           res.onSuccess {
               println("Complete: ${it.complete} :: ${it.rsm}")

               if (!it.complete) {
                   ask(RSM.Query(after = it.rsm!!.last))
               }
           }
       }.consume { forwardedStanza: ForwardedStanza<Message> ->
           if (forwardedStanza.stanza.body != null) println(
               "FROM MAM | ${forwardedStanza.resultId} ${timestampToISO8601(forwardedStanza.timestamp!!)} ${forwardedStanza.stanza.from}: ${forwardedStanza.stanza.body}"
           )
       }.send()
   }
   ask()

Because MAM server has defined maximum amount of returned messages, we have to ask until query is not complete. In example it is done by recurrent execution of method ``ask()`` with filled Result Set Management object. ``RSM.Query(after = it.rsm!!.last)`` means that in result must be included only messages located after last message identifier from currently received package of messages.

Note, that parameter ``with`` of method ``query()`` is optional, so you can ask for all messages since specific time exchanged with specific JID, or you can ask for all messages stored in archive.

The second way to retrieve messages from archive is asking for messages located before or after specific message identifier.

.. code:: kotlin

   val mamModule = halcyon.getModule<MAMModule>(MAMModule.TYPE)
   fun ask(q: RSM.Query? = null) {
       mamModule.query(
           with = "someone@tigase.org",
           rsm = q
       ).response { res ->
           res.onSuccess {
               println("Complete: ${it.complete} :: ${it.rsm}")

               if (!it.complete) {
                   ask(RSM.Query(after = it.rsm!!.last))
               }
           }
       }.consume { forwardedStanza: ForwardedStanza<Message> ->
           if (forwardedStanza.stanza.body != null) println(
               "FROM MAM | ${forwardedStanza.resultId} ${timestampToISO8601(forwardedStanza.timestamp!!)} ${forwardedStanza.stanza.from}: ${forwardedStanza.stanza.body}"
           )
       }.send()
   }
   ask(RSM(after = lastKnownMessageId))

To get MAM related message ID, you have to use ``getStanzaIDBy()`` function (As parameter, you have to put name of own account):

.. code:: kotlin

   val mamMessageId = message.getStanzaIDBy("myaccount@tigase.org".toBareJID())

It is useful when message is received "in normal way", so when it is pushed to client by server.

When message is received as result of using method ``query()`` (in consumer), identifier is given in ``ForwardedStanza``:

.. code:: kotlin

   val mamMessageId = forwardedStanza.resultId

The same mechanism you can use to load older messages in history (not loaded yet by client):

.. code:: kotlin

   mamModule.query(
       with = "someone@tigase.org",
       rsm = RSM.Query(before = "2753e4a8-9150-4e34-8757-4cd5e8419159", max = 20)
   ).response { res ->
       res.onSuccess {
           println("Complete: ${it.complete} :: ${it.rsm}")
       }
   }.consume { forwardedStanza: ForwardedStanza<Message> ->
       println(
           "FROM MAM | ${forwardedStanza.resultId} ${
               timestampToISO8601(
                   forwardedStanza.timestamp!!
               )
           } ${forwardedStanza.stanza.from}: ${forwardedStanza.stanza.body}"
       )
   }.send()

In above example, client is asking for 20 messages located in history before message ``2753e4a8-9150-4e34-8757-4cd5e8419159``.

.. note::

   Message Archive returns requested amount of messages. Not every message may contain body to show. Some of those messages may contain only confirmation of message read or other controlling commands.
