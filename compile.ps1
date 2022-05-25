Write-Host -ForegroundColor Green "generating..."
py gen.py
if($LastExitCode -ne 0){ exit $LastExitCode }
Write-Host -ForegroundColor Green "compiling..."
javac -g out.java
if($LastExitCode -ne 0){ exit $LastExitCode }
if(!(Test-Path lox)){
  New-Item -ItemType Directory -Path lox | Out-Null
}
Move-Item *.class lox -Force
Move-Item out.java lox -Force

jar cfm lox.jar entry lox/*.class
# Remove-Item -Recurse lox
