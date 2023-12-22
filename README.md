`mvn compile assembly:single`

`java -Xmx2G -jar target/void-stranger-1.0-SNAPSHOT-jar-with-dependencies.jar`

```console
ls -1 levels/VoidStranger/simplified | while read line ; do java -jar target/void-stranger-1.0-SNAPSHOT-jar-with-dependencies.jar $line ; done 
```