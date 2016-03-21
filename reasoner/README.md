# Lumen Reasoner

## Requirement: WordNet 3.0 and ind.zip

1. Download WordNet 3.0 database from http://wordnetcode.princeton.edu/3.0/WNdb-3.0.tar.gz
2. Extract to `D:\wn30`. After extraction make sure you have a folder called `D:\wn30\dict`.
3. Download WordNet Bahasa ind from http://compling.hss.ntu.edu.sg/omw/wns/ind.zip
4. Extract `ind.zip` to `D:\wn30`. After extraction make sure you have a folder called `D:\wn30\msa`.

## Requirement RabbitMQ

1. Install otp_win64_18.2.1
2. Install rabbitmq-server-3.6.1

## Building and Running

1. in PostgreSQL, pgAdmin III Create `lumen_lumen_dev` database
2. In `config` folder (not in `src\main\resources` folder), copy `application.dev.properties` to `application.properties` (make it)
3. If you use proxy, you need to edit `application.properties` and enter your proxy address+username+password, from `http.proxyHost` to `https.proxyPort` delete "#" , 
    enter your `spring.datasource.username` and `spring.datasource.password`,  delete "#" ,
4. In `config/agent` folder, copy `(agentId).AgentSocialConfig.dev.json` to `(agentId).AgentSocialConfig.json` (make it) , agent ID : example "arkan"
5. In `config/agent` folder, copy `(agentId).TwitterAuthorization.dev.jsonld` to `(agentId).TwitterAuthorization.jsonld` (make it) , agent ID : example "arkan"
6. click menu `run` choose `edit configuration` in working directory fill with `$MODULE_DIR$` , click ok
7. click reasoner>src>main>java>right click in `ReasonerApp` choose Run



