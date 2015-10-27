# Lumen Reasoner

## Requirement: WordNet 3.0

1. Download WordNet 3.0 database from http://wordnetcode.princeton.edu/3.0/WNdb-3.0.tar.gz
2. Extract to `D:\wn30`. After extraction make sure you have a folder called `D:\wn30\dict`.

## Building and Running

1. In `config` folder, copy `application.dev.properties` to `application.properties`
2. If you use proxy, you need to edit `application.properties` and enter your proxy address+username+password
3. In `config/agent` folder, copy `(agentId).dev.json` to `(agentId).json` (choose your own agent)
