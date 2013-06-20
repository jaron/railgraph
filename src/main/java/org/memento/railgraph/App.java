package org.memento.railgraph;

import java.io.File;
import java.io.IOException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.kernel.impl.util.FileUtils;


/**
 * My first Neo4j connector
 *
 */
public class App 
{
	private static final String DB_PATH = "/Users/jaron/data/neo4j-data/graph.db";
	
    String greeting;
    // START SNIPPET: vars
    GraphDatabaseService graphDb;
    private Index<Node> nodeIndex;
    Node firstNode;
    Node secondNode;
    Relationship relationship;
    // END SNIPPET: vars

    // START SNIPPET: createReltype
    
    // END SNIPPET: createReltype

    public static void main( final String[] args )
    {
        App hello = new App();   
        hello.clearDb();
        hello.createDb();
        hello.populateData();
        // TODO export data to a GraphML file
        // re-enable this if you want to purge the database on shutdown
        // hello.removeData();
        
        // TODO some query that demonstrates data has been stored correctly
        // TODO some analytics
        
        hello.shutDown();
    }

    void createDb()
    {   
        // create a connection to the persistent database 
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
        nodeIndex = graphDb.index().forNodes("nodes");
        System.out.println("Connected to embedded Neo4j database");
        registerShutdownHook( graphDb );
    }
 
    public void populateData()
    {
    	System.out.println("Creating new StationProcessor");
    	StationProcessor processor = new StationProcessor(graphDb, nodeIndex);
    	processor.populate();
    	
    	NodeManager nodeManager = ((GraphDatabaseAPI) graphDb).getDependencyResolver().resolveDependency(NodeManager.class);

    	long currentRelationships = nodeManager.getNumberOfIdsInUse(Relationship.class);
    	System.out.println("relations = " + currentRelationships);
    	
    	long currentNodes = nodeManager.getNumberOfIdsInUse(Node.class);
    	System.out.println("nodes = " + currentNodes);
    	
    	/* for (int i=0; i< currentNodes; i++)
    	{
    		Node foundNode = graphDb.getNodeById(i);
    		Iterable<String> keys = foundNode.getPropertyKeys();
    		Iterator<String> it = keys.iterator();
    		while(it.hasNext())
    		{
    			String key = it.next();
    			String value = (String) foundNode.getProperty(key);
    			System.out.println(i + " : " + key + " = " + value);
    		}
    	} */
    	
    	
        // START SNIPPET: transaction
        /* Transaction tx = graphDb.beginTx();
        try
        {
            // Updating operations go here
            // END SNIPPET: transaction
            // START SNIPPET: addData
            firstNode = graphDb.createNode();
            firstNode.setProperty( "message", "Hello, " );
            secondNode = graphDb.createNode();
            secondNode.setProperty( "message", "World!" );

            relationship = firstNode.createRelationshipTo(secondNode, StationProperties.RelationTypes.CONNECTS);
            relationship.setProperty( "message", "brave Neo4j " );
            // END SNIPPET: addData

            // START SNIPPET: readData
            System.out.print( firstNode.getProperty( "message" ) );
            System.out.print( relationship.getProperty( "message" ) );
            System.out.print( secondNode.getProperty( "message" ) );
            // END SNIPPET: readData

            greeting = ( (String) firstNode.getProperty( "message" ) )
                       + ( (String) relationship.getProperty( "message" ) )
                       + ( (String) secondNode.getProperty( "message" ) );

            tx.success();
        }
        finally
        {
            tx.finish();
        } */
        // END SNIPPET: transaction
    }

    private void clearDb()
    {
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

    /* void removeData()
    {
        Transaction tx = graphDb.beginTx();
        try
        {
            // START SNIPPET: removingData
            // let's remove the data
            firstNode.getSingleRelationship( RelTypes.CONNECTS, Direction.OUTGOING ).delete();
            firstNode.delete();
            secondNode.delete();
            // END SNIPPET: removingData

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    } */

    void shutDown()
    {
        System.out.println();
        System.out.println( "Shutting down database ..." );
        // START SNIPPET: shutdownServer
        graphDb.shutdown();
        // END SNIPPET: shutdownServer
    }

    // START SNIPPET: shutdownHook
    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }
    // END SNIPPET: shutdownHook

}
