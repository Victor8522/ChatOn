# JavaChat — Application de chat multi-clients

Application de chat style Discord en Java (Swing + Sockets + SQLite).

## Structure du projet

```
JavaChat/
├── src/
│   ├── common/
│   │   ├── Message.java       — Objet échangé client↔serveur (Serializable)
│   │   └── Password.java      — Hachage SHA-256 des mots de passe
│   ├── server/
│   │   ├── Server.java        — Serveur multi-clients (TCP, port 10000)
│   │   ├── DatabaseManager.java — Accès SQLite (login, messages, groupes)
│   │   ├── InitDB.java        — Initialisation de la base de données
│   │   └── AddUserToGroups.java — Utilitaire : ajouter un user à tous les groupes
│   └── client/
│       ├── Main.java          — Point d'entrée client
│       ├── Client.java        — Couche réseau (connexion, envoi, écoute)
│       ├── LoginWindow.java   — Fenêtre de connexion / inscription
│       └── Window.java        — Fenêtre principale (style Discord)
├── lib/
│   └── sqlite-jdbc-3_7_2.jar  — Driver SQLite
├── compile.sh / compile.bat
├── run_server.sh / run_server.bat
└── run_client.sh / run_client.bat
```

## Prérequis

- **Java 21** (JDK) installé et dans le PATH

## Démarrage rapide

### 1. Compiler

**Linux / Mac :**
```bash
chmod +x compile.sh run_server.sh run_client.sh
./compile.sh
```

**Windows :**
```bat
compile.bat
```

### 2. Initialiser la base de données (une seule fois)

**Linux / Mac :**
```bash
java -cp out:lib/sqlite-jdbc-3_7_2.jar server.InitDB
```

**Windows :**
```bat
java -cp out;lib\sqlite-jdbc-3_7_2.jar server.InitDB
```

Comptes de test créés : `alice/alice`, `bob/bob`, `charlie/charlie`

### 3. Lancer le serveur

**Linux / Mac :**
```bash
./run_server.sh
```
**Windows :**
```bat
run_server.bat
```

### 4. Lancer le(s) client(s)

**Linux / Mac :**
```bash
./run_client.sh
```
**Windows :**
```bat
run_client.bat
```

Ouvrir autant de terminaux que de clients souhaités.

## Protocole réseau

| Sens          | Objet envoyé                  | Description                        |
|---------------|-------------------------------|------------------------------------|
| Client→Serveur | `String` username             | Authentification (1/2)             |
| Client→Serveur | `String` passwordHash         | Authentification (2/2)             |
| Serveur→Client | `Integer` userId              | -1 si échec                        |
| Serveur→Client | `Map<Integer,String>` groupes | Groupes de l'utilisateur           |
| Serveur→Client | `String` "ONLINE:a,b,c"       | Liste des connectés                |
| Client→Serveur | `String` "HISTORY:groupId"    | Demande d'historique               |
| Serveur→Client | `List<Message>` historique    | Réponse historique                 |
| Client→Serveur | `Message`                     | Envoi d'un message                 |
| Serveur→Client | `Message` (enrichi)           | Broadcast aux membres du groupe    |

## Fonctionnalités

- Authentification par hash SHA-256 (mot de passe jamais envoyé en clair)
- Inscription de nouveaux comptes
- Groupes de discussion (canaux style Discord)
- Historique des messages (50 derniers par canal)
- Liste des membres connectés en temps réel
- Interface graphique Swing style Discord
- Sauvegarde des messages en SQLite
