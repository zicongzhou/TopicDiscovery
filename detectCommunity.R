library(igraph);

dat <- scan("/media/netdisk/zzhou/temp/net.dat",what=list(0,0,0));
edges <- cbind(dat[[1]],dat[[2]]);
weight <- dat[[3]];
g <- graph.edgelist(edges);
E(g)$weight <- weight;
tg <- as.undirected(simplify(g),mode="collapse");
fg <- fastgreedy.community(tg,weights=weight);
cm <- community.to.membership(tg,fg$merges,steps=which.max(fg$modularity)-1);

dat1 <- read.table("/media/netdisk/zzhou/temp/vob_net.dat");
words <- as.vector(dat1[,3]);
for(i in 0:max(cm$membership)){
group <- i;
cgIdx <- as.integer(V(tg)[cm$membership==group]);
delV <- V(tg)[cm$membership!=group];
cg <- delete.vertices(tg,delV);
#cgMeasure <- evcent(cg);
cgMeasure <- degree(cg);
topIndex <- cgIdx[order(cgMeasure,decreasing=TRUE)[1:10]];
topWords <- words[topIndex];
print(group)
print(topWords)
}
