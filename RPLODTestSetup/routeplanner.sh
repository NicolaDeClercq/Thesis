#!/bin/bash
cd routeplanner
echo hours=200 > config.properties
echo walkingDistance=1000 >> config.properties
echo updateInterval=200 >> config.properties
echo \"url\",\"name\" > rplodservers.txt
#echo \"http://rplod.noip.me:50001\",\"NMBS\" >> rplodservers.txt
#echo \"http://rplod.noip.me:50002\",\"De Lijn\" >> rplodservers.txt
#echo \"http://rplod.noip.me:50003\",\"TEC\" >> rplodservers.txt
#echo \"http://rplod.noip.me:50004\",\"MIVB\" >> rplodservers.txt
echo \"http://localhost:50001\",\"NMBS\" >> rplodservers.txt
echo \"http://localhost:50002\",\"De Lijn\" >> rplodservers.txt
#echo \"http://localhost:50003\",\"TEC\" >> rplodservers.txt
#echo \"http://localhost:50004\",\"MIVB\" >> rplodservers.txt
#java -Xmx9g -jar turtle.jar
java -Xmx9g -jar csv.jar