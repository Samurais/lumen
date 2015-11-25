@echo off
java -Xms256m -Xmx256m -cp target/dependency/*;target/classes org.lskk.lumen.socmed.LumenSocmedApp %*
