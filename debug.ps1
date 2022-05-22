.\compile.ps1
if($LastExitCode -ne 0){ exit $LastExitCode }
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 -jar lox.jar $args
