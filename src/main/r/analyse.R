library(igraph)

set.seed(123456)

# change this to your working directory (or where you want the graphs downloaded)
setwd("/users/jaron/Documents/workspace/railgraph/graphs")

fileName = "tubeDLR.graphml"
if (file.exists(fileName)) {
  print("Graph file already exists")
} else {
  print("Downloading graph file")
  dest =  paste("https://github.com/jaron/railgraph/blob/master/graphs/", fileName, sep="") 
  download.file(dest, destfile=fileName, method="curl")
}
g = read.graph(fileName, format="graphml")

# obtain summary information about the graph
summary(g)

layoutFR <- layout.fruchterman.reingold(g, niter=600, area=vcount(g)^1.9, repulserad=vcount(g)^3)
plot(g, layout=layoutFR, vertex.size=V(g)$degree, vertex.label=NA)

# degree.distribution should give you the number of nodes with each specific edge count
range(degree(g, mode="total"))
degree.distribution(g, T)

# calculate the in and out degrees separately
# use the degree() function and options for calculating directed degree
# see the documentation here: http://igraph.sourceforge.net/documentation.html
degreeVal = degree(g)

# see which node has the max degree
V(g)$label[which.max(degreeVal)]
V(g)$label[which(degreeVal == 7)]
V(g)$label[which(degreeVal == 6)]


# find undirected betweenness (betweenness()) scores and then nodes with the max betweenness
# warning, can be slow with large graphs, you may consider betweenness.estimate instead

bb = betweenness(g)
V(g)$label[which.max(bb)]
# show neighbours
V(g)$label[V(g)[nei(which.max(bb))]]


# Leverage centrality calculation taken from http://igraph.wikidot.com/r-recipes#toc10
lvcent <- function(graph){
  k <- degree(graph)
  n <- vcount(graph)
  sapply(1:n, function(v) { mean((k[v]-k[neighbors(graph,v)]) / (k[v]+k[neighbors(graph,v)])) })
}
lv = lvcent(g)

# for reference, this calculates the PageRank of stations but there's no obvious correlation here
# after all, connections between stations are driven by geography and cost of building lines, however desirable superhub stations might be
pr = page.rank(g)
V(g)$label[which.max(pr$vector)]
V(g)$label[V(g)[nei(which.max(pr$vector))]]

# merge data values into a single data frame
df <- data.frame(V(g)$label, bb, degreeVal, lv)
col.names=c("Station", "Betweenness", "Degree", "Leverage")
names(df) <- col.names

options(digits=2)
df[ order(-df[,2], df[,1]), ]









