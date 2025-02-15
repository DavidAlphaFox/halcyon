MUCModule
----------

This module implements `XEP-0045: Multi-User Chat <https://xmpp.org/extensions/xep-0045.html>`__.

Events
^^^^^^^

There are two kinds of events in ``MUCModule``:

1. Room related events.

2. Other events

Currently the only event from second category is ``InvitationReceived``:

::

   halcyon.eventBus.register<MucEvents.InvitationReceived>(MucEvents.TYPE) { event ->
       println("${event.invitation.sender} invites you to room ${event.invitation.roomjid}")
   }

All room related events contains ``Room`` object, and all have common parent:

::

   halcyon.eventBus.register<MucRoomEvents>(MucRoomEvents.TYPE) { event ->
       when(event){
           is MucRoomEvents.YouJoined -> println("You joined to room ${event.room.roomJID}")
           is MucRoomEvents.OccupantCame -> println("Occupant ${event.nickname} came to ${event.room.roomJID}")
           is MucRoomEvents.OccupantLeave -> println("Occupant ${event.nickname} leaves room ${event.room.roomJID}")
           // ...
       }
   }


``InvitationReceived``
~~~~~~~~~~~~~~~~~~~~~~

Event fired when an invitation is received. Contains ``Invitation`` object.


``YouJoined``
~~~~~~~~~~~~~~~~~~~~~~

Event fired when server responses for join request with success.


``YouLeaved``
~~~~~~~~~~~~~~~~~~~~~~

Event fired, when you leave room. It may be confirmation of you leave request, or you are kicked out from room.

``JoinError``
~~~~~~~~~~~~~~~~~~~~~~

Event fired when server not accepted join request.


``Created``
~~~~~~~~~~~~~~~~~~~~~~

Event informs that room you joined is just created (by join request).

``OccupantCame``
~~~~~~~~~~~~~~~~~~~~~~

Informs that new occupant joined to room.


``OccupantLeave``
~~~~~~~~~~~~~~~~~~~~~~

Informs that occupant leaves the room.

``OccupantChangedPresence``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Informs that occupant updated his presence.

``ReceivedMessage``
~~~~~~~~~~~~~~~~~~~~~~

Event fired when group chat message from room is received.

Methods
^^^^^^^^

``join(roomJID, nickname, password)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Builds join request to MUC Room.

Here is simple example to show how to join to room.

::

    halcyon.eventBus.register<MucRoomEvents.YouJoined>(MucRoomEvents.TYPE) {
        println("You joined to room ${it.room.roomJID} as ${it.nickname}")
    }

   mucModule.join("coven@chat.shakespeare.lit".toBareJID(), "thirdwitch").send()

Note, that because of MUC protocol specificity, confirmation of join will be delivered as separated event.


``leave(room: Room)``
~~~~~~~~~~~~~~~~~~~~~~

Builds request to leaves MUC Room.


``destroy(room: Room)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Builds room destroy request.


``invite(room: Room, invitedJid: BareJID, reason: String? = null)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Builds mediated invitation request.


``inviteDirectly(room: Room, invitedJid: BareJID, reason: String? = null)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Builds direct invitation request.

``retrieveRoomConfig(room: Room)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Builds retrieve room configuration request. In response it returns data form with configuration.

``updateRoomConfig(room: Room, form: JabberDataForm)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Builds update room configuration request.

``message(room: Room, msg: String)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Builds group chat message request.

``decline(invitation: Invitation, reason: String? = null)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Builds decline request for received invitation

``accept(invitation: Invitation, nickname: String)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Builds join request to MUC Room based on received invitation.

``retrieveAffiliations(room: Room, filter: Affiliation? = null)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Builds request for retrieve affiliations list from MUC room. In response it returns collection of RoomAffiliation.

``updateAffiliations(room: Room, affiliations: Collection<RoomAffiliation>)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Builds request for update affiliations list.

``updateRoomSubject(room: Room, subject: String?)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Builds request for set room subject.

``ping(room: Room)``
~~~~~~~~~~~~~~~~~~~~~~

Build request for self ping, as described in `XEP-0410: MUC Self-Ping (Schrödinger’s Chat) <https://xmpp.org/extensions/xep-0410.html>`__.

Store
^^^^^^

MUCModule requires Room Storage to store rooms data. By default, Halcyon comes with in-memory store.

To build own Store you have to implement this interface:

::

   interface MUCStore {

       fun findRoom(roomJID: BareJID): Room?

       fun createRoom(roomJID: BareJID, nickname: String): Room
   }

Remember, that MUC protocol is not suitable to mobile clients, so ``state`` of room join (in ``Room`` object) may not represent real state. For example, after reconnection client may keep state ``Joined`` but server received information about disconnection and removes occupant from room. To check if rejoin is required, please use ``ping(room)`` function.
