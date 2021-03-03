all: clean compile

compile:
	mvn -q compile assembly:single


#run: compile
#	java -ea -jar target/cspSampling-1.0-SNAPSHOT-jar-with-dependencies.jar

#package:
#	mvn -q clean package

#oldrun: package
#	mvn -q -e exec:java -D exec.mainClass=org.mvavrill.cspSampling.TestMain

clean:
	mvn -q clean
