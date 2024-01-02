mvn compile assembly:single
ls -1 levels/VoidStranger/simplified | while read line ; do java -jar target/void-stranger-1.0-SNAPSHOT-jar-with-dependencies.jar $line ; done 
