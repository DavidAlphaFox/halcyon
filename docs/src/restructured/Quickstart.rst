Quickstart
==========

Simplest client
---------------

Here is example of simplest client sending one message.

**SimplestClient.kt.**

.. code:: kotlin

   val halcyon = Halcyon()
   halcyon.configuration.let {
       it.setJID("client@tigase.net".toBareJID())
       it.setPassword("secret")
   }
   halcyon.connectAndWait()

   halcyon.request.message {
       to = "romeo@example.net".toJID()
       "body"{
           +"Art thou not Romeo, and a Montague?"
       }
   }.send()

   halcyon.disconnect()

Handling changes of connection status
-------------------------------------

We can listen for changing status of connection:

.. code:: kotlin

   halcyon.eventBus.register<HalcyonStateChangeEvent>(HalcyonStateChangeEvent.TYPE) { stateChangeEvent ->
       println("Halcyon state: ${stateChangeEvent.oldState}->${stateChangeEvent.newState}")
   }

Available states:

-  ``Connecting`` - this state means, that method ``connect()`` was called, and connection to server is in progress.

-  ``Connected`` - connection is fully established.

-  ``Disconnecting`` - connection is closing because of error or manual disconnecting.

-  ``Disconnected`` - Halcyon is disconnected from XMPP server, but it is still active. It may start reconnecting to server automatically.

-  ``Stopped`` - Halcyon is turned off (not active).
