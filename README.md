# My Music Application
Il s'agit d'une application permettant de lire de la musique


# Fonctionnalités
- Un service permettant de lire de la musique
- Couper la musique lorsque l'application perd le focus du son
- Afficher les musiques / albums dans un recyclerView
- Momorisation des listes de lecture (et bien d'autre) à l'aide de fragment et de parcel
- Notification permattant de changer l'état de la chanson jouer
- Une popup affichant trois options : Trouver la pochette de l'album correspondant et la télécharger (via l'API Spotify), Editer la pochette avec une photo présente sur l'appareil, Modifier les informations de la chanson voulue. Ces options ne modifies pas les metadonnées en elles-mêmes, les changements disparaîteront à la fermeture de l'applicaion.

![alt tag](https://raw.githubusercontent.com/ChrisMichelGit/INF4042_michel_depaillat/master/Illustration.png)

# Notes importantes
Cette application à principalement été testée sur un appareil physique API 23 (un téléphone One Plus 3). Elle devrait normalement tourner sur des API 17 au plus bas.
Pour que cette application ait de l'intérêt il faut que l'utilisateur possède des musiques sur son téléphone et qu'il donne les droits d'accès à l'application
