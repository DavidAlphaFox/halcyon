SIMSModule
-----------

This module implements partially `XEP-0385: Stateless Inline Media Sharing (SIMS) <https://xmpp.org/extensions/xep-0385.html>`__. It describes file sharing metadata.

``SIMSModule`` isn’t module in Halcyon architecture sense. This is rather collection of extensions to help retrieve and generate SIMS structures.

Methods
^^^^^^^^

``getReferenceOrNull()``
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Method returns ``Reference`` object if exists.

``getMediaSharingFileOrNull()``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This method (extends ``Reference``) returns shared file details.

``createFileSharingReference()``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This method creates complete ``Reference`` object with shared file details.
