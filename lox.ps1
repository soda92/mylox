mvn -q package

if ($LASTEXITCODE -eq 0) {
    java -jar .\build\mylox-1.0.jar $args
}
