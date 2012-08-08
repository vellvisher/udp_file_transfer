Protocol Description :
The Client sends a SYN to the ServerDispatcher.
ServerDispatcher makes a new Server thread.
Server thread responds with SYN.
Client sends filename with segment index 0.
Server sends ACK 1.
Client sends segments with indices starting from 1 - n.
Server send ACK from 2 - (n + 1).
Client sends FIN.
Server sends FIN.
Connection terminates.

Notes :
Tester class provided can be used to generate files full of random bytes of random size and can also be used to automatically initiate clients for each file in testFiles directory.
