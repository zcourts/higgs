# Boson protocol specification
The Boson protocol is fairly simple.

# A message

* A message is a series of bytes...
* A Message encapsulates a few parts, separated by a pipe |  symbol.
* __1__ The first section of a message is its length, the length is the size of the array of bytes in the message contents
* __2__ The second section is a comma separated list of characters(a-zA-Z0-9) used to specify options. Currently  only the character 'm' is in use which specifies a multi-part message, 0 is used, indicating no options and the whole message is included
* __3__ The third section is the contents of the message. For example...

# Example

1024|0|topic|bytes_here

* The above represents a message in its entirety where
* __1024__ is how many bytes the message contains,
* __0__ means no options
* __topic__ is obviously the topic of the message, if 'topic' is not set then the | __must__ still be passed
* __bytes_here__ would be the 1024 bytes of the message.

# Example 2

1024|0||bytes_here

* Here, no topic is associated with the message but the prefix and suffix | are still included

# Example

__a__ 1024|m|topic|bytes_here
__b__ 600|m|topic|bytes_here
__c__ 50|0|topic|bytes_here

* In __a__ and __b__, we've specified the m flag which means there is more to come
*
# Requests

*

# Response

*