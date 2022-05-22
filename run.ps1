.\compile.ps1
if($LastExitCode -ne 0){ exit $LastExitCode }
java -jar lox.jar $args
