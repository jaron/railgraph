package org.memento.railgraph;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * This tests the Neo4j connection is working properly
 */
public class GraphDbTest
{
    protected GraphDatabaseService graphDb;
    protected Index<Node> nodeIndex;

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
        graphDb.shutdown();
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
    public void testCreateNode()
    {
    	String testRef = "Edgware_Road_tube_station_(Circle,_District_and_Hammersmith_%26_City_Lines)";
        // START SNIPPET: unitTest
        Transaction tx = graphDb.beginTx();

        Node n = null;
        try
        {
            n = graphDb.createNode();
            n.setProperty(StationProperties.stationReference.name(), testRef);
            nodeIndex.add(n, StationProperties.stationReference.name(), testRef);
            tx.success();
        }
        catch ( Exception e )
        {
            tx.failure();
        }
        finally
        {
            tx.finish();
        }

        // The node should have an id greater than 0, which is the id of the reference node.
        assertThat( n.getId(), is( greaterThan( 0L ) ) );

        // Retrieve a node by using the id of the created node. The id's and property should match.
        Node foundNode = graphDb.getNodeById( n.getId() );
        assertThat( foundNode.getId(), is( n.getId() ) );
        assertThat( (String) foundNode.getProperty(StationProperties.stationReference.name()), is(testRef) );
        
        Node fetchNode = nodeIndex.get(StationProperties.stationReference.name(), testRef).getSingle();
        assertThat( fetchNode.getId(), is( n.getId() ) );
        assertThat( (String) fetchNode.getProperty(StationProperties.stationReference.name()), is(testRef) );
        
        Node fakeNode = nodeIndex.get(StationProperties.stationReference.name(), "FAKE").getSingle();
        assertNull(fakeNode);
        
        // END SNIPPET: unitTest
    }
    
    @Test
    public void testCreateConnection()
    {
    	String station1Ref = "Edgware_Road_tube_station_(Circle,_District_and_Hammersmith_%26_City_Lines)";
    	String station2Ref = "London_Paddington_station";
        
        Transaction tx = graphDb.beginTx();

        Node edgwareRoad = null;
        Node paddington = null;
        try
        {
        	edgwareRoad = graphDb.createNode();
        	edgwareRoad.setProperty(StationProperties.stationReference.name(), station1Ref);
            nodeIndex.add(edgwareRoad, StationProperties.stationReference.name(), station1Ref);
            
            paddington = graphDb.createNode();
            paddington.setProperty(StationProperties.stationReference.name(), station2Ref);
            nodeIndex.add(paddington, StationProperties.stationReference.name(), station2Ref);
            
            assertFalse(edgwareRoad.hasRelationship());
            edgwareRoad.createRelationshipTo(paddington, RelationTypes.CONNECTS);
            assertTrue(edgwareRoad.hasRelationship());
            assertTrue(paddington.hasRelationship());
            Iterable<Relationship> results = edgwareRoad.getRelationships(RelationTypes.CONNECTS);
            Iterator<Relationship> it = results.iterator();
            while (it.hasNext()) {
				Relationship r = (Relationship) it.next();
				System.out.println(r.getStartNode() + " --> " + r.getEndNode());
			}
            
            tx.success();
        }
        catch (Exception e) {
            tx.failure();
        }
        finally {
            tx.finish();
        }

        Node fetchNode = nodeIndex.get(StationProperties.stationReference.name(), station1Ref).getSingle();
        assertThat( fetchNode.getId(), is( edgwareRoad.getId() ) );
        assertThat( (String) fetchNode.getProperty(StationProperties.stationReference.name()), is(station1Ref) );
    }
    
    
}
