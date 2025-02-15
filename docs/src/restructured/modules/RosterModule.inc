RosterModule
-------------

Module is reponsible for keeping and managing roster items.

Events
^^^^^^^

``ItemAdded``
~~~~~~~~~~~~~~

Fired when new item is added to roster.

``ItemUpdated``
~~~~~~~~~~~~~~~~

Fired when item is modified.

``ItemRemoved``
~~~~~~~~~~~~~~~~~

Fired when item is removed from roster.

Methods
^^^^^^^^

``addItem(vararg items: RosterItem)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Method prepares request which add or update item to roster. When server confirms action, event ``ItemAdded`` or ``ItemUpdated`` will be fired.


``deleteItem(vararg jids: BareJID)``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Method prepares request to remove item from roster. When server confirms operation, event ``ItemRemoved`` will be fired.

``getAllItems()``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Method returns add known roster items.

Implementing own storage
^^^^^^^^^^^^^^^^^^^^^^^^^^^

RosterModule supports roster versioning, but it requires custom implementation of ``RosterStore`` to allow storing roster locally. By default, Halcyon has in-memory roster store. To do that, there is interface ``tigase.halcyon.core.xmpp.modules.roster.RosterStore`` what need to be extend. To use custom implementation of ``RosterStore`` simply put it to property ``store`` in ``RosterModule``. Note, that it have to be done before login.
