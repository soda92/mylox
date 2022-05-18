javac lox.java
if [ $? -eq 0 ]
then
  [ ! -d "lox" ] && mkdir lox
  mv *.class lox
  jar cfm lox.jar Manifest.txt lox/*.class
else
  exit $?
fi

if [ $? -eq 0 ]
then
  java -jar lox.jar $@
fi
