javac lox.java
if($LastExitCode -eq 0){
  if(!(Test-Path lox)){
    New-Item -ItemType Directory -Path lox | Out-Null
  }
  Move-Item *.class lox
}
else{
  exit $LastExitCode
}

jar cfm lox.jar Manifest.txt lox/*.class

if ($LastExitCode -eq 0) {
    java -jar lox.jar $args
}
