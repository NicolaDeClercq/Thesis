#A route planning system for public transport that uses Linked Open Data
This repository contains all files and code used in my thesis. Files or folders with files larger than 100 MB were zipped because GitHub doesn't accept files larger than 100 MB.

This thesis describes the development of a route planning system for public transport that can plan routes using timetable data from different public transport companies, without the existence of connections between the data of the different companies. For this system it is crucial that it is easy to add new datasets and that the whole system remains scalable as the route planning acts on more datasets. A Linked Open Data model will be created that can describe the data in an efficient and integratable way. After that, data from some public transport companies will be mapped to that model and a system will be developed to make this data available for route planners in a quick and efficient manner. It is the speed at which the data can be made available, that determines how up to date these datasets are, and therefore how quickly the latest information about delays can be used in the route planner. As proof of concept, a route planning application will also be constructed to test if it is possible to do route planning for public transport in reasonable time using Linked Open Data.

This repository is divided into six folders:
* /Datadumps contains the sample data dumps of the stops and departures for NMBS and De Lijn for a whole day in Turtle and Linked CSV format. More information about these dumps can be found in the file info.txt.
* /RDFMapperGTFS contains the code of the mapper program for GTFS data.
* /RDFMapperNMBS contains the code of the mapper program for NMBS data.
* /RDFRoutePlanner contains the code of the route planning program
* /RPLOD contains the RDF scheme of the RPLOD ontology.
* /RPLODTestSetup contains all files needed to reproduce the results of the evaluation.

To start some RPLOD servers, the script start.sh is provided in the folder RPLODTestSetup. It needs 2 arguments, the date in numeral notation (20150110 for January 10, 2015), and the number of hours after the time of updating, the departures have to be fetched for. By commenting some java command lines in the script, one can choose which server(s) he wants to start. In the script some properties file will be created for each server, containing some configuration parameters. If the GTFS data in the servers is out of date, new GTFS data can be fetched from the website of iRail: https://hello.irail.be/gtfs.

To start a route planner instance, the script routeplanner.sh can be used. It doesn't need any arguments and by (un)commenting lines that write to the file rplodservers.txt, one can choose the URLs of the RPLOD servers the route planner will use. In the script a properties file will be created, containing some configuration parameters. In the script, one can choose to start a route planner that fetches data in Turtle or Linked CSV format, by starting the JAR turtle.jar or csv.jar.

The script compression_check.sh contains code to fetch the same RPLOD data in 4 different ways from the same server, and shows the file size for each way. The 4 ways are Turtle, Turtle + GZIP, Linked CSV and Linked CSV + GZIP. The script takes one argument, the URL of the RPLOD server.

If one wants to compile the source code to fresh JARs, only Maven is needed for that.
