# Makefile for Proxy and Server

# Compiler and flags
JAVAC = javac
JAVA = java
JFLAGS = -g -Xlint:unchecked

# Directories
SRC_DIR = .
LIB_DIR = ../lib

# Classpath setup
CLASSPATH = $(SRC_DIR):$(LIB_DIR)

# Source files
PROXY_SRC = Proxy.java
SERVER_SRC = Server.java
INTERFACE_SRC = RmiInterface.java

# Target classes
PROXY_CLASS = Proxy
SERVER_CLASS = Server

# Hardcoded arguments
PROXY_ARGS1 = 127.0.0.1 11122 ~/private/DS/coursework-distributed-systems/15440-p2/src/tmp/proxy1 5500000 # 100000
PROXY_ARGS2 = 127.0.0.1 11122 ~/private/DS/coursework-distributed-systems/15440-p2/src/tmp/proxy2 5500000 # 100000
SERVER_ARGS = 11122 ~/private/DS/coursework-distributed-systems/15440-p2/src/tmp/server

# Default target
all: $(PROXY_CLASS).class $(SERVER_CLASS).class

# Rule to compile .java files into .class files
%.class: %.java
	$(JAVAC) $(JFLAGS) -cp $(CLASSPATH) $<

# Target to run the proxy with hardcoded arguments
run-proxy1: $(PROXY_CLASS).class
	$(JAVA) -cp $(CLASSPATH) $(PROXY_CLASS) $(PROXY_ARGS1)

# Target to run the proxy with hardcoded arguments
run-proxy2: $(PROXY_CLASS).class
	$(JAVA) -cp $(CLASSPATH) $(PROXY_CLASS) $(PROXY_ARGS2)

# Target to run the server with hardcoded arguments
run-server: $(SERVER_CLASS).class
	$(JAVA) -cp $(CLASSPATH) $(SERVER_CLASS) $(SERVER_ARGS)

# Clean up compiled files
clean:
	rm -f *.class *.jar

.PHONY: all run-proxy run-server clean