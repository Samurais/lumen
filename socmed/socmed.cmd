@echo off
java -Xms256m -Xmx256m -Djava.awt.headless=true -cp target/dependency/*;target/classes org.lskk.lumen.socmed.LumenSocmedApp %*
