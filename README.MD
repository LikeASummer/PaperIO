### Module Geometry
Contain all common geometry classes that are used in the game

### Module Game Logic
Contains game logic classes (all the game rules and constraints)

### Module ML
Contain machine learning related classes together with latest ML model 

### Module Desktop
Desktop game UI. Works as execution root for singleplayer/multiplayer clients

### Module Singleplayer
Game implementation for one user where all competitors are bots

### Module Multiplayer
GRPC client implementation to communicate with server and show all necessary data  on players screen

### Module Server:
GRPC Multiplayer server implementation.
Players are connecting to common lobby then when number of players reach 8 or (30 seconds passes) new game session is created and players move to that session to play. (If number of players is lower than 8, bots will take vacant places)
