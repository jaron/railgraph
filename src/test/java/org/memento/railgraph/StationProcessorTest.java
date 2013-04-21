package org.memento.railgraph;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * Unit test for StationProcessor
 */
public class StationProcessorTest extends TestCase
{
    /* public StationProcessorTest(String testName) {
        super( testName );
    }

    public static Test suite() {
        return new TestSuite(StationProcessor.class);
    } */

	@Test
	public void testReferences()
	{
		String input = "/wiki/Blackfriars_station#London_Underground";
		String output = StationProcessor.makeReference(input);
		assertEquals("Blackfriars_station", output);
	}
	
	@Test
	public void testDocumentParsing() throws Exception
	{
		String dataDir = "src/test/resources/wiki";
		File dataFileSRA = new File(dataDir, "Stratford_station");
		Document doc = Jsoup.parse(dataFileSRA, "UTF-8");
		String stationName = doc.getElementById("firstHeading").text();
		assertEquals("Stratford station", stationName);
		
		String geoData = doc.select(".geo").text();
		assertNotNull(geoData);
		String latitude = geoData.substring(0, geoData.indexOf(";")).trim();
		String longitude = geoData.substring(geoData.indexOf(";") + 1).trim();
		
		System.out.println("GeoData = " + latitude + " N, " + longitude + " W");
	}
	
    /**
     * Test that we can process a typical station description
     */
	@Test
    public void testProcessStation() throws Exception
    {
		String dataDir = "src/test/resources/wiki";
		// the following stations are edge cases 
    	File dataFileBA = new File(dataDir, "Balham_station");
    	File dataFileLST = new File(dataDir, "Liverpool_Street_station");
    	File dataFileEB = new File(dataDir, "Ealing_Broadway_station");
    	File dataFileER = new File(dataDir, "Edgware_Road_tube_station_(Circle,_District_and_Hammersmith_%26_City_Lines)");
    	File dataFileSRA = new File(dataDir, "Stratford_station");
    	File dataFileLEY = new File(dataDir, "Leyton_tube_station");
    	
        StationProcessor processor = new StationProcessor();
        
        Vector<Connections> connections = processor.processStation(dataFileBA, "/wiki/Balham_station");
        assertNotNull(connections);
        assertEquals(2, connections.size());
        
        
        
        
        connections = processor.processStation(dataFileLST, "/wiki/Liverpool_Street_station");
        assertEquals(10, connections.size()); // 5 national rail, 4 tube lines and Crossrail
        
        connections = processor.processStation(dataFileEB, "/wiki/Ealing_Broadway_station");
        assertEquals(5, connections.size()); // 3 national rail, 2 tube lines
        
        // TODO add fix for district line
        connections = processor.processStation(dataFileER, "/wiki/Edgware_Road_tube_station_(Circle,_District_and_Hammersmith_%26_City_Lines)");
        assertEquals(3, connections.size()); // 3 tube lines (actually 4)
        
        connections = processor.processStation(dataFileSRA, "/wiki/Stratford_station");
        assertEquals(13, connections.size()); 
        
        connections = processor.processStation(dataFileLEY, "/wiki/Leyton_tube_station");
        assertEquals(1, connections.size()); 
        // TODO verify all connections resolve to known nodes
        
        Iterator<Connections> it = connections.iterator();
		int i = 0;
		while(it.hasNext())
		{
			Connections c = it.next();
			System.out.println(i + "\t" + c.toString());
		}
        
    }
}
