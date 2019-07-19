# 2019-net-k

## Description


For this project we were brought to realize an application that allows the transfer and download of files inspired by BiTorrent to implement the peer to peer system, our application contains a hub "old tracker in BiTorrent" which contains the list of players and communicates to the player at his request, our system contains players "old peers" who can exchange files with other players, and book "old piece" which is a part of the stuff "the data that we will share" and finally a library "old torrent file" which contain a description of the stuff, the address of the hub, the name and size of the stuff, the size used for books,and books Hash.

## Instructions to run, build and test the project

* **To Build :** You have just to clone the project from the git repository in your IDE. 
* **To Run :** First you have to run the HubServerLuncher the the player interface so you can download or upload a file.

## The architecture

### Player to hub

The player send an update message to the hub to announce their presence and to obtain a list of other available players. This message is incoded in a JSON format and sent then as a byte array to the hub.

### Hub to player
This message is sent regularly by the hub as a reply to the player message. It is also a JSON formatted message sent as a byte array,and it containt the list of other available players.
### Player to player
* **REQUEST :** Whenever the a player is downloading it uses an “algortihm” to choose the next book to be downloaded from which player.After that it contacts the desired player that have the book using this message.It sends a message in JSON format on the ip and port that the desired player is listening on.
* **BITFIELD :** When a player recieve the list from the hub, it connects to the players present on this list. After the connection is established, all these players will send a bitfield message to the player that connected to them.
*  **Have :** When a player finishes downloading a book and has validated it against the checksum, it sends the have message to all the players that it is currently connected to, to say it now has the book.
*  **PIECE :** When a player recieves a REQUEST message from another player, it sends the requested books to the player who is asking for it.
 
