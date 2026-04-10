#!/bin/bash
cd "$(dirname "$0")"
java -cp out:lib/sqlite-jdbc-3_7_2.jar client.Main
