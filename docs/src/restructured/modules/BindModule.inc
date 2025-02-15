BindModule
-----------

Resource Binding module. This module is responsible for resource binding
as described in `RFC <https://xmpp.org/rfcs/rfc6120.html#bind>`__.

Properties
^^^^^^^^^^^

-  ``boundJID`` - contains full JID bound during resource binding
   process, or null if client isn’t logged in and/or not binded.

Methods
^^^^^^^^^^^

There is no reason to call methods from this module in client. This
module is used internally by Halcyon library.

``bind(String)``
~~~~~~~~~~~~~~~~~~

Method prepares request to bind resource. As parameters it gets proposed
resource name or ``null`` if resource name should be generated by
server. In response it returns object ``BindResult`` contains full bound
JID.
