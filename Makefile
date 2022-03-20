repertory_class=bin
repertory_src=src/mini_c
lib=lib/java-cup-11a-runtime.jar 

all: 
	javac -d $(repertory_class) -classpath $(lib) $(repertory_src)/*.java