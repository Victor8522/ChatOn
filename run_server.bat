@echo off
cd /d "%~dp0"
if not exist chat.db (
    echo === Initialisation de la base de donnees ===
    java -cp out;lib\sqlite-jdbc-3_7_2.jar server.InitDB
)
echo === Demarrage du serveur ===
java -cp out;lib\sqlite-jdbc-3_7_2.jar server.Server
pause
