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


# Multi part messages (m)

As demonstrated below, multi part messages are sent as separate messages. The first (and all following messages except the last) part of the message
specifies the size,m flag and the data. The end of the multi-part message sends the same structure but setting the m flag to 0.
If __no data is left__ then the last frame can also specify a size of 0,the flag 0 and no topic or data parts, as in __0|0__
__NOTE__ Multi part messages __are buffered__ and on the receiving end until the end message is received then __ALL__ the contents of the buffer is passed to the receiver as __ONE MESSAGE__!
If you need multi-part messages delivered as separate messages just as they are received then see __Split multi message__

# Example

__a__ 1024|m|topic|bytes_here
__b__ 600|m||bytes_here
__c__ 50|0||bytes_here

* In __a__ and __b__, we've specified the m flag which means there is more to come
* In __c__ however, we've set the flag to 0 indicating that this is the end of the multi part message.

# Split multi message (mm)

With a multi-part message, messages are buffered and delivered as a single message. This is not always desirable and it may be the case
that you want multi part messages delivered as separate messages, in the order they are received, once all parts are received.
To accomplish this, simple change your first frame to use the flag __'mm'__ instead of just 'm'

# Example

__a__ 1024|mm|topic|bytes_here
__b__ 600|mm||bytes_here
__c__ 50|0||bytes_here

__mm__ flag causes messages to be delivered separately but still in the same order they were received.

# Requests

*

# Response

*