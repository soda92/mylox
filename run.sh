python gen.py
if [ ! $? -eq 0 ]; then exit $? ; fi
javac out.java
if [ ! $? -eq 0 ]; then exit $? ; fi
[ ! -d "lox" ] && mkdir lox
mv *.class lox
mv out.java lox
jar cfm lox.jar entry lox/*.class
if [ ! $? -eq 0 ]; then exit $? ; fi
java -jar lox.jar $@
