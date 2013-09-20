//  Pubsub envelope publisher
//  Note that the zhelpers.h file also provides s_sendmore

#include "zhelpers.h"

int mainaxa (void)
{
    //  Prepare our context and publisher
    void *context = zmq_ctx_new ();
    void *publisher = zmq_socket (context, ZMQ_PUB);
    zmq_bind (publisher, "tcp://*:5563");

    while (1) {
        //  Write two messages, each with an envelope and content
        //s_sendmore (publisher, "A");
        //s_send (publisher, "We don't want to see this");
        //s_sendmore (publisher, "B");
        s_send (publisher, "B We would like to see this, but it is a very long message so we want the subscriber to be able to handle longer frames with the length encoded as a long type and this long ass message should do the trick. One can only hope...But just in case we must keep going, jut to be sure that this is actually gets encoded as a 64bit length type frame. Hopefully ZMQ is as awesome as people proclaim and this should work right?");
        //sleep (1);
    }
    //  We never get here, but clean up anyhow
//    zmq_close (publisher);
//    zmq_ctx_destroy (context);
    return 0;
}
