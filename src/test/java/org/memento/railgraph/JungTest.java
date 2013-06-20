package org.memento.railgraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.kernel.impl.util.FileUtils;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;

import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;
import edu.uci.ics.jung.algorithms.scoring.HITS;

import static org.junit.Assert.*;


/**
 * This tests interoperability with Neo4j and the JUNG library
 */
public class JungTest
{
    protected GraphDatabaseService graphDb;
    protected Index<Node> nodeIndex;
    
    private static final String DB_PATH = "/tmp/neo4j-test-db";

    /**
     * Create temporary database for each unit test.
     */
    // START SNIPPET: beforeTest
    @Before
    public void prepareTestDatabase()
    {
        graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        nodeIndex = graphDb.index().forNodes("nodes");
    }
    // END SNIPPET: beforeTest

    /**
     * Shutdown the database.
     */
    // START SNIPPET: afterTest
    @After
    public void destroyTestDatabase()
    {
        if (graphDb != null) graphDb.shutdown();
        try
        {
        	System.out.println("Deleting existing database contents in order to recreate them");
            FileUtils.deleteRecursively( new File( DB_PATH ) );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    // END SNIPPET: afterTest

    
    @Test
    public void startWithConfiguration()
    {
        // START SNIPPET: startDbWithConfig
        Map<String, String> config = new HashMap<String, String>();
        config.put( "neostore.nodestore.db.mapped_memory", "10M" );
        config.put( "string_block_size", "60" );
        config.put( "array_block_size", "300" );
        GraphDatabaseService db = new TestGraphDatabaseFactory()
            .newImpermanentDatabaseBuilder()
            .setConfig( config )
            .newGraphDatabase();
        // END SNIPPET: startDbWithConfig
        db.shutdown();
    }

    
    @Test
    public void testJungAlgorithms() throws Exception
    {
    	// version 2 - create a neo4j database, connect to it, and run jung on its contents
    	
    	Graph gn = new Neo4jGraph(DB_PATH);
    	File file = new File("graphs/tubeDLR.graphml");
    	System.out.println("Loading: " + file.getCanonicalFile());
    	assertTrue(file.exists());
    	
    	InputStream input = new FileInputStream(file);
    	
    	GraphMLReader reader = new GraphMLReader(gn);
    	reader.inputGraph(input);
    	System.out.println("GraphML file loaded");
    	
    	edu.uci.ics.jung.graph.Graph<Vertex, Edge> jungGraph = new GraphJung<Graph>(gn);
    	
    	System.out.println("EDGES = " + jungGraph.getEdgeCount());
    	System.out.println("VECTICES = " + jungGraph.getVertexCount());
    	
    	HITS<Vertex, Edge> scoring = new HITS<Vertex, Edge>(jungGraph);
    	scoring.evaluate();
    	
    	// feel free to experiment with various clustering algorithms
    	int edgesToRemove = jungGraph.getEdgeCount() / 4;
    	EdgeBetweennessClusterer<Vertex, Edge> ebc = new EdgeBetweennessClusterer<Vertex, Edge>(edgesToRemove);
    	Set<Set<Vertex>> results = ebc.transform(jungGraph);
    	
    	int cluster = 1;
    	Iterator<Set<Vertex>> it = results.iterator();
    	while (it.hasNext()) 
    	{
    		System.out.println("---- CLUSTER " + cluster + " ----");
			Set<Vertex> s = (Set<Vertex>) it.next();
			List<Vertex> sortedResults = sortByDegrees(s, jungGraph, false);
			Iterator<Vertex> sIt = sortedResults.iterator();
			while (sIt.hasNext()) {
				Vertex v = (Vertex) sIt.next();
				int degree = jungGraph.degree(v);
				System.out.println(">> " + v.getId().toString() + " [" + degree + "] " + v.getProperty("label") + " (" + scoring.getVertexScore(v) + ")");
			}
			cluster++;
		}
    	
    	HashMap<Vertex, Double> scores = new HashMap<Vertex, Double>();
    	Iterator<Vertex> it2 = jungGraph.getVertices().iterator();
    	while (it2.hasNext()) {
			Vertex v = (Vertex) it2.next();
			// System.out.println(v.getProperty("label") + " = " + pr.getVertexScore(v));
			Double d = new Double(scoring.getVertexScore(v).hub);
			scores.put(v, d);
		}
    	
    	System.out.println("\nTop 20 most hub authorative stations \n");
    	List<Map.Entry<Vertex,Double>> sortedResults = entriesSortedByValues(scores, false);
    	Iterator<Map.Entry<Vertex,Double>> it3 = sortedResults.iterator();
    	int limit = 20, counter = 1;
    	while (it3.hasNext()) {
    		counter++;
    		Map.Entry<Vertex,Double> m = (Map.Entry<Vertex,Double>) it3.next();
			System.out.println(m.getKey().getProperty("label") + " = " + m.getValue().toString());
			if (counter >= limit) break;
		}
    }
    
    
    public static List<Map.Entry<Vertex,Double>> entriesSortedByValues(Map<Vertex, Double> map, final boolean ascending) 
	{
		List<Map.Entry<Vertex,Double>> list = new LinkedList<Map.Entry<Vertex,Double>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Vertex,Double>>() {
			public int compare(Map.Entry<Vertex,Double> e1, Map.Entry<Vertex,Double> e2) 
			{
				if (ascending)
					return e1.getValue().compareTo(e2.getValue());
				else
					return e2.getValue().compareTo(e1.getValue());
			}
		});	
		return list;
	}
   
    /** Utility method to sort vertices by their degree (connectivity) */
	public static List<Vertex> sortByDegrees(final Set<Vertex> nodes, final edu.uci.ics.jung.graph.Graph<Vertex,Edge> gj, final boolean ascending) 
	{
		List<Vertex> list = new LinkedList<Vertex>(nodes);
		Collections.sort(list, new Comparator<Vertex>() {
			public int compare(Vertex e1, Vertex e2) {
				if (ascending)
					return (new Integer(gj.degree(e1)).compareTo(new Integer(gj.degree(e2))));
				else
					return (new Integer(gj.degree(e2)).compareTo(new Integer(gj.degree(e1))));
			}
		});	
		return list;
	}
    
}
