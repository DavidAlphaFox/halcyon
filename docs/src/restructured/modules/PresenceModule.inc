PresenceModule
---------------

Module for handling received presence information.

Events
^^^^^^^

``PresenceReceivedEvent``
~~~~~~~~~~~~~~~~~~~~~~~~~~

Fired when any Presence stanza is received by client.

Fields:

-  ``jid`` - Stanza sender JID.

-  ``stanzaType`` - Presence stanza type.

-  ``stanza`` - Whole received presence stanza.

``ContactChangeStatusEvent``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Fired when Presence stanza is received but it contains different set of fields:

-  ``jid`` - Bare JID of contact.

-  ``status`` - Human readable status set by contact.

-  ``presence`` - Current "best" presence stanza, based on presence priority.

-  ``lastReceivedPresence`` - Just received presence stanza.

Note that ``presence`` in this event may contain stanza received long time ago. Current event is caused by receiving presence from entity with lower priority.

Methods
^^^^^^^^

``getPresenceOf(jid: JID)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Returns presence of given entity or null if never received presence from this entity.

``getBestPresenceOf(jid: BareJID)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Returns best known presence of given bare JID.

``sendPresence(jid: JID?, type: PresenceType?, show: Show?, status: String)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Sends presence stanza to specific jid.


``sendSubscriptionSet(jid: JID, presenceType: PresenceType)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Method for quick send response for subscription request.
