#include "zhelpers.h"

int main (void)
{
    void *context = zmq_ctx_new ();

    //  Socket to send messages on
    void *sender = zmq_socket (context, ZMQ_PUSH);
    zmq_bind (sender, "tcp://*:5557");

    printf ("Press Enter when the workers are ready: ");
    getchar ();

    //  Initialize random number generator
    srandom ((unsigned) time (NULL));

    char *string = "A";

    while(1) {
        s_send (sender, string);
    }

    zmq_close (sender);
    zmq_ctx_destroy (context);
    return 0;
}
