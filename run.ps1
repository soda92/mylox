mvn -q package

if ($LASTEXITCODE -eq 0) {
    java -jar .\build\mylox-trunk.jar $args
}
