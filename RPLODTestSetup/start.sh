#!/bin/bash
cd NMBS
echo port=50001 > config.properties
echo updateInterval=60 >> config.properties
java -Xmx2g -jar *.jar > ../NMBS.log &

cd ../De\ Lijn
echo port=50002 > config.properties
echo updateInterval=60 >> config.properties
echo agency=De Lijn >> config.properties
echo date=$1 >> config.properties
echo hours=$2 >> config.properties
java -Xmx5g -jar *.jar > ../DeLijn.log &

cd ../TEC
echo port=50003 > config.properties
echo updateInterval=60 >> config.properties
echo agency=TEC >> config.properties
echo date=$1 >> config.properties
echo hours=$2 >> config.properties
java -Xmx3g -jar *.jar > ../TEC.log &

cd ../MIVB
echo port=50004 > config.properties
echo updateInterval=60 >> config.properties
echo agency=MIVB >> config.properties
echo date=$1 >> config.properties
echo hours=$2 >> config.properties
java -Xmx2g -jar *.jar > ../MIVB.log &
