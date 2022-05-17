mvn -q package

if [ $? -eq 0 ]
then
  java -jar build/mylox-trunk.jar $@
fi
