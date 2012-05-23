__The Boson protocol is significantly changing during the port from Scala to Java__

A rubbish.crlog.higgs.Message
---------

A message is made up of three main parts. The first part of a message is the protocol version,
the second part is the size of the message and the third part is a list of properties.
A property is a key, value pair and in the context of a message is made up of four parts,
the size of the key followed by the key, the size of the value followed by the value.

__NOTE__

+ Size of the message is inclusive of the protocol version and the size of portion of property
keys and values. Of course, property keys and values themselves are also included.

+ The size of the protocol is limited to the first 2 bytes of a message (Always is).

+ This size is limited to Integer.Max_value in Java, roughly 2GB,
  or the **first 4 bytes of a message**, i.e. a 32 bit signed integer

# Number of properties

# rubbish.crlog.higgs.Message properties

+ __Property key size__  is limited to 16 bits, i.e __2 bytes__ so 2 bytes extra for each property key

+ __Property key__  This is the actual key for a property

+ __Property value size__  Like the size of a message the value of a property is limited to Integer.MAX_VALUE
                         So each property can be up to about 2GB in size, however, the size of a message
                         is limited to the same amount. This means only 1 property can be anywhere near this size.
                         The size of protocol version flag must also be deducted from this.

+ __Property value__    The contents of the property that the size component describes.

# Serialization & The Order of Properties

When serializing, the following order MUST be used.

1. The protocol version
2. The size of the message
3. All other flags, the order of the other flags is undefined.
4. All user specific properties such as topic,content and other custom properties.


Flags
-----

Flags are also properties themselves but are prefixed with "hbf" i.e. Higgs Boson flag.

# Protocol Version

Every message sent must send the version of the protocol that was use to encode that message.
This will lead to backwards compatibility as the protocol and implementations evolve.

A recommended approach is for the component responsible for "reading off the wire" to de-serialize
only the first 2 bytes (A 'short' in Java) which will be the protocol version.
Using that version all other decoding should be delegated to version specific protocol de-serializers.