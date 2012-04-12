nectj: ./src/nectj.java
	javac ./src/*.java
	jar cfe nectj-build.jar src.nectj src
	@echo NECTJ interpreter built.
	@echo Run with \'./nectj \<input file\>\'

clean:
	rm ./src/*.class
	rm ./*.jar
