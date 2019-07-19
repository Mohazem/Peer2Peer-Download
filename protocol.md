# Protocol specification

## 1) Torrent File Structure

Library files, also known as Torrents, are encoded in a JSON format. The extension of a library file is .libr and they contain the following keys:
* **IP** - The IP address of the tracker (hub). (string)
* **Port** - The port of the tracker (hub). (integer)
* **Version** - The version number of the protocol. In case of different versions, backwards compatibility must be secured. (integer)
* **File (Stuff) Name** - The suggested name of the shared file described by the current torrent file. (string)
* **File (Stuff) Size** - The total length of the shared file in bytes. (integer)
* **Piece (Book) Size** - The size in bytes of each piece the file is split into. Usually it is a power of 2 (ie. 256K). (integer)
* **Pieces (Book) Hash** - List containing SHA1 hashes for each piece the file is split into. (string array)

## 2) Peer to Tracker Communication

### 2.1) Description

The Client uses the tracker’s IP and PORT obtained from the Torrent file to contact the Tracker. It will contact the tracker at regular amounts of time, indicated by the “interval” field of the tracker’s message, to confirm its presence to the tracker, otherwise it will be discarded from the tracker’s list of peers.

If there is a special event (a stop, a pause, a resume, a report of a bad peer), the client can send a message to the tracker even sooner than the interval specified by the tracker.

Every time the Tracker receives a message from a Client, it will insert / update him in the list of all Peers and send this list to the Client which sent the message.

If the “left” field is 0 in the message from the Client, then it means this Client finished already so no peers list is sent to him.

If the event field is “report”, there is no response to this Client, but the response message will be sent to the reported bad citizen.

### 2.2) Tracker Update (Peer -> Tracker)
##### *2.2.1) What programs send what messages to what other program?*
The peers send an update message to the tracker to announce their presence and to obtain a list of other available peers. This message is incoded in a JSON format and sent then as a byte array to the tracker.
##### *2.2.2) When each message is sent?*
These messages are sent once every “interval” seconds and they act both as a sign up and as a refresher for the Tracker.
##### *2.2.3) What is the content of each exchanged message?*

* **peer_id** - a random string each peer uses to identify itself to the tracker and by which can be looked up in the mapping table. -> string
* **ip** - the ip address of the peer / the ip address of the reported peer in case of a report event -> string
* **port** - the port address of the peer / the port address of the reported peer in case of a report event -> integer
* **left** - number of bytes this peer has to download until it finishes the entire file. This is particularly important! If it is set to 0, the Tracker will not reply with a peer list anymore. -> integer
* **event (optional)** - It can be either: STARTED, STOPPED, PAUSED, REPORT, NONE. Default: NONE. -> string

### 2.3) Peer Update (Tracker -> Peer)
##### *2.3.1) What programs send what messages to what other program?*
This message is sent regularly by the Tracker as a reply to the “Tracker Update” message. It is also a JSON formatted message sent as a byte array.
##### *2.3.2) When each message is sent?*
After each Tracker Update Message.
##### *2.3.3) What is the content of each exchanged message?*
* **interval** - The number of seconds a Peer should wait between consecutive requests. -> integer
* **peers** -  A list of peers available from who the current Peer can download pieces. Each peer is formed as a Tuple (peer_id, peer_ip, peer_port) -> string array

## 3) Peer to Peer Communication

### 3.1) Description
The communication between peers has the main goal of downloading a specific file. It’s a two way handshake communication where peers exchange pieces which are the chunks belonging to the shared file. Each peer will be downloading and uploading pieces at the same moment. The peer will use its list of peers which it has got from the tracker to request the desired pieces. A peer will be listening to incoming connections to handle sending the requested pieces.Peers will be exchanging pieces until each peer has all the file.

### 3.2) Messages
##### 3.2.1) REQUEST-Message:
###### *i) When and how is this message sent?*
Whenever the a peer is downloading it uses an “algortihm” to choose the next piece to be downloaded from which peer.After that it contacts the desired peer that have the piece using this message.It sends a message in JSON format on the ip and port that the desired peer is listening on.
###### *ii) What’s the content of this message?*
* message type (number): This parameter is for the reciever to know what’s this message about.
* index: The index of the requested piece.

##### 3.2.2) BITFIELD-Message:
###### *i) When is this message sent?*
When a peer recieve the list from the tracker, it connects to the peers present on this list. After the connection is established, all these peers will send a bitfield message to the peer that connected to them.
###### *ii) What’s the content of this message?*
* type (number): This parameter is for the reciever to know what’s this message about.
* array of bits: An array indicating what the peer has. For example: 01001 indicates that the peer has pieces 1 and 4, but it doesn’t has pieces 0, 2, and 3.

##### 3.3.3) HAVE-Message:
###### *i) When is this message sent?*
When a peer finishes downloading a piece and has validated it against the checksum, it sends the have message to all the peers that it is currently connected to, to say it now has the piece.
###### *ii) What’s the content of this message?*
* type (number)(1 byte): This parameter is for the receiver to know what’s this message about.
* index(number)(4 bytes): The index of the piece that the sender has just successfully downloaded.

##### 3.3.4) PIECE-Message:
###### *i) When is this message sent?*
When a peer recieves a REQUEST message from another peer, it sends the requested pieces to the peer who is asking for it.
###### *ii) What’s the content of this message?*
* message type (number): This parameter is for the reciever to know     what’s this message about.
* index : The index of the requested piece.
* block of data: Block of data, which is the piece specified by index.

## 4) Messages
### 4.1) Client - Tracker
	i) send(jsonTextify(“message_type: ”add_to_list”, client_ip_addr:”192.167.0.0”, client_sv_port:74231”))
	ii) send(jsonTextify(“message_type: “whose_there”, “number_of_desired_peers”:10 <optional>))
	iii) send(jsonTextify(“message_type: “report”, client_ip_addr:”192.167.0.0”, “report_reason”:0 <optional>))

### 4.2) Tracker - Client
	i) response(jsonTextify(“response_type: add_to_list“, “result: 0/1”, “comment: Bla bla” <optional>)
	ii) response(jsonTextify( “response_type: whose_there“,“result: 0/1”, “comment: Bla bla” <optional>, peers: { peer1:{ ”ip_sv_addr, port_sv”}, peer2:{”ip_sv_addr, port_sv”}, … } )


### 4.3) Client - Server
HAVE:

	send(<len=0005>; <id=4>; <piece index>)
REQUEST:

	send (<len=0005><id=6><index>)



### 4.4) Server - Client
BITFIELD:

    send(<len=0001+X><id=5><bitfield>)
PIECE:

	<len=0005+X><id=7><index><block>
