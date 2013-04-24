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
has been created, which you can load into a graph exploration tool like [Gephi](http://gephi.org).

You can download the graph as a [Gephi project](https://github.com/jaron/railgraph/blob/master/graphs/tubeDLR.gephi?raw=true) or a [GraphML file](https://raw.github.com/jaron/railgraph/master/graphs/tubeDLR.graphml)


####Creating your own graphs####

To create a graph from Wikipedia you'll need the following:

* Java (and Maven to build)
* [Neo4j](http://neo4j.org) - a graph database

To run the analysis code you'll need:

* R - I recommend [RStudio](http://rstudio.org)
* install the iGraph and VGAM R packages

Step by step intructions on running the code will be coming soon.


- - -

This is free software: you can redistribute it and/or modify it under the terms of the [GNU General Public License](http://www.gnu.org/licenses/gpl.html).

The graph data files and images are made available under a [Creative Commons Attribution-ShareAlike license](http://creativecommons.org/licenses/by-sa/3.0/deed.en_US), you are free to use and modify them for any purposes, as long as attribution as preserved and any derivative works are also unrestricted.
