#!/bin/bash
SCRIPT_DIR="$(dirname $0)"
java -Xms256m -Xmx256m -cp $SCRIPT_DIR'/target/classes':$SCRIPT_DIR'/target/dependency/*' org.lskk.lumen.reasoner.ReasonerApp "$@"
