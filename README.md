##RAILGRAPH##

####Create graphs of railways####

***

Creator: [Jaron Collis](http://memento.org)

This code allows you to harvest transport information from Wikipedia and use it to construct a node-edge graph, 
where the nodes are stations and edges are connections between them. 

Also included are some scripts written in R that will allow you to perform some useful social network analyses 
on a node-edge graph stored in GraphML format.


####Getting Started####

You might want to begin by seeing what [results](https://raw.github.com/jaron/railgraph/master/graphs/tubeDLR.png) this program can produce. A graph of the London Underground network 
has been created, which you can load into a graph exploration tool like [Gephi](http://gephi.org), you can download it either as a 
[Gephi project](https://github.com/jaron/railgraph/blob/master/graphs/tubeDLR.gephi?raw=true) or a [GraphML file](https://raw.github.com/jaron/railgraph/master/graphs/tubeDLR.graphml)

.

####Creating your own graphs####

The code that creates graphs from Wikipedia is written in Java, and has a Maven build configuration. It uses an 
embedded [Neo4j](http://neo4j.org) graph database, so you shouldn't need to install it separately, (although if you're interested in working with graphs, I'd recommend it).

Once you've downloaded the code, edit the file App.java and change the path to where you want your Neo4j data files to be stored. You can point this to an existing Neo4j database if you like.

Next build it, a few tests have been included to check it's working as expected. The build command is simply:

> mvn 

Now, you can run it:

> java -cp target/railgraph-1.0-jar-with-dependencies.jar org.memento.railgraph.App

The first time you run it, it might take a minute, as the application will download the Wikipedia pages for all the stations into the data directory.
When you run it subsequently, it will use the local copies. After fetching the data it will extract information on stations and their connections and store them as nodes and edges 
in the Neo4j database. At the end of the run it will display an omissions list: these are connections that have been found where one of the stations doesn't have a corresponding node. 
Typically these will be national rail stations, and represent the boundary of the area the graph covers.

Your rail network is now in your Neo4j database, if you have the web interface installed, you can begin exploring it. An even better way is to install [Gephi](http://gephi.org) 
and the [Neo4j plugin](https://marketplace.gephi.org/plugin/neo4j-graph-database-support/) and import your graph. You can then export it in a graph format of your choosing; I'd recommend
GraphML for its compatibility with igraph. Bear in mind that Neo4j stores data as directed graphs, so when you export it, specify it as undirected, and the duplicate reciprocal edges will be omitted.
Note: there seems to be a bug in the Neo4j import process, so once you export to GraphML I recommend closing and restarting Gephi, and reloading your newly created GraphML file.  

When you view your newly created graph, it might look rather jumbled. To position the stations according to their true geographically location, simply install the 
[GeoLayout plugin](https://marketplace.gephi.org/plugin/geolayout/). The gotcha is Neo4j import process treats all data as Strings, but the GeoLayout expects numeric values. To address this, 
duplicate the latitude and longitude columns in the Data Laboratory panel so they're of type Double, then choose the 'Geo Layout' option in the main graph view, setting the 
latitude and longitude options to the 2 new columns you've just created. The nodes should now appear with their proper orientation. You can then choose another 
layout, like Force Atlas, to push the nodes apart and make the graph easier to read.

.

####Analysing your graphs#####

The analysis code is written in R, and can be found in the src/main/r directory. If you haven't got it already, I'd recommend installing [RStudio](http://rstudio.org).
Before running the code, install the igraph and VGAM packages from the R repository. 
Then simply edit the R code to change the working directory (look for the line setwd), and run it. 

If social network analysis is new to you, the results are discussed in [this document](https://dl.dropboxusercontent.com/u/10449719/AnalysisOfTheLondonUnderground.pdf).

And there's links to some research papers relating to the study of transport networks on the [project wiki](https://github.com/jaron/railgraph/wiki).

Do get in touch (contactme @ jaroncollis . com) if you've any questions.

.

- - -

This is free software: you can redistribute it and/or modify it under the terms of the [GNU General Public License](http://www.gnu.org/licenses/gpl.html).

The graph data files and images are made available under a [Creative Commons Attribution-ShareAlike license](http://creativecommons.org/licenses/by-sa/3.0/deed.en_US), you are free to use and modify them for any purpose, as long as attribution is preserved and any derivative works are also unrestricted.
