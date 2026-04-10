@echo off
cd /d "%~dp0"
mkdir out 2>nul
echo === Compilation ===
javac -cp lib\sqlite-jdbc-3_7_2.jar -d out ^
  src\common\Message.java ^
  src\common\Password.java ^
  src\server\DatabaseManager.java ^
  src\server\InitDB.java ^
  src\server\AddUserToGroups.java ^
  src\server\Server.java ^
  src\client\Client.java ^
  src\client\Window.java ^
  src\client\LoginWindow.java ^
  src\client\Main.java
echo === Compilation terminee ===
pause
