#!/bin/bash
cd "$(dirname "$0")"
# Initialiser la BD si elle n'existe pas
if [ ! -f chat.db ]; then
    echo "=== Initialisation de la base de données ==="
    java -cp out:lib/sqlite-jdbc-3_7_2.jar server.InitDB
fi
echo "=== Démarrage du serveur ==="
java -cp out:lib/sqlite-jdbc-3_7_2.jar server.Server
