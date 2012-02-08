# Boson protocol specification
The Boson protocol is fairly simple.

# A message

* A message is a series of bytes...
* A Message encapsulates a few parts.
* __1__ The first section of a message is its length, the length is the size of the array of bytes in the message contents.
There is a limit on the size of your message. The limit is Integer.Max_value in Java, roughly 2GB, or
the first 4 bytes of a message, i.e. a 32 bit signed integer
* __2__ The second section is a flag, a flag is __one byte__ immediately following the message size.
Currently  only 0x0,0x1 and 0x2 is in use which specifies a no more content, multi-part and split multi-part messages respectively,
* __3__ The third section is the length of the message's topic... this is limited to 16 bits, i.e __2 bytes__
* __4__ The fourth section is the bytes that represent the topic
* __5__ The fifth section is the bytes that make up the actual message contents

# Flags

* __0x0__(__NO_MORE_CONTENT__) Full message with no more content
* __0x1__(__MULTI_PART_MESSAGE__) Multi-part message which is delivered as a single message on the receiving end, a 'single' message here means
a single Message object containing all the contents of the multiple message parts received.
* __0x2__(__SPLIT_MULTI_PART_MESSAGE__) Split Multi-part message which delivers each part of a multi part message separately on
the receiving end __once all parts of the message has been received__.
* __0x3__(__NO_MESSAGE_BUFFER__) The same as split multi-part message with the exception of when messages are delivered.
Instead of buffering, the parts of a split multi-part message are delivered as soon as they are received

__NOTE__ Split messages cannot have multiple topics. Only the topic of the first part received is considered. This means that
if you send 2 parts that are meant to be put into one message, the topic of the first message is used for the resulting object of combining the parts.


# Example

1024 0x0 10 topic message_here

* The above represents a message in its entirety (note that space is added to make it readable, no spaces are sent) where

__1024__ is how many bytes the message contains,

__0x0__ means no more content

__10__ is the size of the topic

__topic__ is obviously the topic of the message, if 'topic' is not set then nothing goes here, _but_ the topic size must beset to 0

__message_here__ would be the 1024 bytes of the message.

__________________________________to be edited to reflect current implementation________________________________________

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
that you want multi part messages delivered as separate messages, in the order they are received , __once all parts are received__ (__or not__).
To accomplish this, simply change your first frame to use the flag __'mm'__ instead of just 'm'

# Example

__a__ 1024|mm|topic|bytes_here

__b__ 600|mm||bytes_here

__c__ 50|0||bytes_here

__mm__ flag causes messages to be delivered separately but still in the same order they were received.

# Example

__a__ 1024|mm,s|topic|bytes_here

__b__ 600|mm||bytes_here

__c__ 50|0||bytes_here

By setting a second flag, 's' split multi part messages are no longer delivered at the end once all parts of the message is received,
instead each part of the message is delivered, __without__ buffering. i.e. each part of the message is delivered separately and instantly without waiting for the end of the split multi part message.
