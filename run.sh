#!/usr/bin/env bash

if ! python gen.py;
then exit $?
fi

if ! javac out.java;
then exit $?
fi

[ ! -d "lox" ] && mkdir lox

mv *.class lox
mv out.java lox

if ! jar cfm lox.jar entry lox/*.class;
then exit $?
fi

java -jar lox.jar "$@"
