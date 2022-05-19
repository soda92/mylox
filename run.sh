py gen.py
if [ ! $? -eq 0 ]; then exit $? ; fi
javac out.java
if [ ! $? -eq 0 ]; then exit $? ; fi
[ ! -d "lox" ] && mkdir lox
mv *.class lox
jar cfm lox.jar Manifest.txt lox/*.class
if [ ! $? -eq 0 ]; then exit $? ; fi
java -jar lox.jar $@
