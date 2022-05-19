py gen.py
if($LastExitCode -ne 0){ exit $LastExitCode }
javac out.java
if($LastExitCode -ne 0){ exit $LastExitCode }
if(!(Test-Path lox)){
  New-Item -ItemType Directory -Path lox | Out-Null
}
Move-Item *.class lox -Force

jar cfm lox.jar Manifest.txt lox/*.class

if($LastExitCode -ne 0){ exit $LastExitCode }
java -jar lox.jar $args
