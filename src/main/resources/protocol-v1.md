__The Boson protocol is significantly changing during the port from Scala to Java__

A info.crlog.higgs.Message
---------

A message is made up of two main parts. The first part of a message is the number of properties the message has and
the second part is a list of properties.
A property is a key, value pair and in the context of a message is made up of four parts,
the size of the key followed by the key, the size of the value followed by the value.

# Number of properties

The first part of a message is how many properties the message has.
This size is limited to Integer.Max_value in Java, roughly 2GB,
or the **first 4 bytes of a message**, i.e. a 32 bit signed integer

# info.crlog.higgs.Message properties

+ __Property key size__  is limited to 16 bits, i.e __2 bytes__ so 2 bytes extra for each property key

+ __Property key__  This is the actual key for a property

+ __Property value size__  Like the number of properties in a message the value of a property is limited to Integer.MAX_VALUE
                         So each property can be up to about 2GB in size

+ __Property value__    The contents of the property that the size component describes.

# Serialization & The Order of Properties

When serializing, the following order MUST be used.

1. The number of properties the message has
2. The first property which must be the protocol version
3. All other flags, the order of the other flags is undefined. What's important is protocol version is the very first
4. All user specific properties such as topic,content and other custom properties.


Flags
-----

Flags are also properties themselves but are prefixed with "hbf" i.e. Higgs Boson flag so for example, the version of
the protocol is given as hbfv --> 1 i.e. hbfv is the property name and 1 is the value.

# Protocol Version

Every message sent must send the version of the protocol that was use to encode that message.
This will lead to backwards compatibility as the protocol and implementations evolve.

A recommended approach is for the component responsible for "reading off the wire" to de-serialize
only the properties size and the first property which would be the protocol version.
Using that version all other decoding should be delegated to version specific protocol de-serializers.

The protocol key must be a string, four characters 'hbfv', and the value must be a one decimal place float
i.e 1.0 where 1 is the major version, 0 is the revision version. Between major versions there may not be much
change in the protocol that would warrant a version change, i.e. the change/s does not break compatibility.

Given that, all implementations should read the property size then read the first property and interpret the key as a
string, as in, what you'd get when you perform getBytes() on a Java string. And at the same time interpret the value
as a float.