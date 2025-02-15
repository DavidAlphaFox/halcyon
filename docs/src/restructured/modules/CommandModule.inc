CommandModule
--------------

This module implements `XEP-0050: Ad-Hoc Commands <https://xmpp.org/extensions/xep-0050.html>`__.

Methods
^^^^^^^^

``retrieveCommandList``
~~~~~~~~~~~~~~~~~~~~~~~~

Retrieves list of commands allowed to execute on given XMPP entity. Because this command is just wrapper to ``DiscoveryModule.items()`` method, in response it returns ``DiscoveryModule.Items`` class.


``retrieveCommandInfo``
~~~~~~~~~~~~~~~~~~~~~~~~

Retrieves detailed information about specified command. Because this command is just wrapper to ``DiscoveryModule.info()`` method, in response it returns ``DiscoveryModule.Info`` class.


``executeCommand``
~~~~~~~~~~~~~~~~~~~~~~~~

This method executes Ad-hoc Command on XMPP entity specified by JabberID.

Arguments:

-  ``jid`` - Jabber ID of command executor,

-  ``command`` - command name,

-  ``form`` - optional element containing data form,

-  ``action`` - command action,

-  ``sessionId`` - session identifier, if command is executed in session (identifier is generated by executor).

As response method returns object of ``AdHocResult`` class. It contains result form (optional) and status of command execution.

.. code:: kotlin

   val module = halcyon.getModule<CommandsModule>(CommandsModule.TYPE)
   module.executeCommand("responder@domain".toJID(), "configure").response {
       it.onSuccess { result ->
           println("Status: ${result.status}")
           println("Form: ${result.form}")
       }
   }.send()

If this command creates session, we can simply use data from ``result`` to execute next command in it:

.. code:: kotlin

   result.form.getFieldByVar("password").fieldValue = "1234"
   module.executeCommand(result.jid, result.node, result.form.createSubmitForm(), null, result.sessionId).response {
       it.onSuccess { result ->
           when(result.status){
               Status.Completed -> println("Configured")
               Status.Canceled -> println("Command canceled")
               Status.Executing -> println("Configuration is not finished yet. Next step in session ${result.sessionId} is required.")
           }
       }
   }.send()

Above example uses default action in second step (``null`` on arguments list).

``registerAdHocCommand(command: AdHocCommand)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In addition to executing commands on other XMPP entities, module allows to register ad-hoc commands, to be executed in client by others.

Command must implement ``AdHocCommand`` interface.

.. code:: kotlin

   class TestAdHoc : AdHocCommand {

       override fun isAllowed(jid: BareJID): Boolean = jid == "owner@example.com".toBareJID()

       override val node: String = "command-node-name"
       override val name: String = "Example command"

       override fun process(request: AdHocRequest, response: AdHocResponse) {
           response.form = createForm()
           response.notes = arrayOf(Note.Info("Everything is OK"))
           response.status = Status.Completed
       }
   }

Above sample command may be executed only by ``owner@example.com``. It even be hidden on commands list for others.

Created command must be registered in ``CommandModule``:

.. code:: kotlin

   module.registerAdHocCommand(TestAdHoc())

Ad-hoc commands supports sessions. Session allows to store some data in session context and creates multiple stages commands. By default sessions are not started automatically. To access to session in command, use method ``request.getSession()``. This method returns current session context or creates new one if it is necessary.

.. code:: kotlin

   class SessionTestAdHoc : AdHocCommand {

       override fun isAllowed(jid: BareJID): Boolean = jid == "owner@example.com".toBareJID()

       override val node: String = "example-session-adhoc"
       override val name: String = "Example session command"

       override fun process(request: AdHocRequest, response: AdHocResponse) {
           var counter = request.getSession().values["stage"] as Int? ?: 0
           ++counter
           request.getSession().values["stage"] = counter
           if (counter < 3) {
               response.notes = arrayOf(Note.Info("Step $counter"))
               response.actions = arrayOf(Action.Next)
               response.defaultAction = Action.Next
               response.status = Status.Executing
           } else {
               response.notes = arrayOf(Note.Info("Finished"))
               response.status = Status.Completed
           }
       }
   }

If response status is ``Completed`` or ``Canceled``, then session context is destroyed after command execution.

.. note::

   Remember, that single instance of Ad-Hoc Command may process calls from many callers.
