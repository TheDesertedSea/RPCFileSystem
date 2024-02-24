all: clean proxy

util/*.class: 
	javac -d . util/*.java

server/*.class: util/*.class
	javac -d . server/*.java

proxy: server/*.class
	javac -d . proxy/*.java

clean:
	rm -f *.class
