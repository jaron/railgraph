package org.memento.railgraph;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This harvests station information from Wikipedia and converts it into a node-edge graph that can be persisted 
 * in a Neo4j database.
 * 
 * @author jaron
 */

public class StationProcessor 
{
	private static String dataDirName = "data";

	// TODO add Overground stations and CrossRail

	private static String defaultPrefix = "/wiki/";
	private static String defunctCategory = "/wiki/Category:Defunct_railroads";
	private static String terminus = "Terminus";
	private static String former   = "Former";
	private static String outOfSystem = "Out of system";
	private static String transfer = "Transfer";
	private static String abandoned = "Abandoned";
	private static String disused = "Disused";
	private static String historic = "Historic";
	private static String serviceArrangement = "Service arrangement";

	// reference to a Neo4j database
	private GraphDatabaseService graphDb;
	private Index<Node> nodeIndex;

	protected static Hashtable<String, String> synonyms = new Hashtable<String, String>();
	protected static String[] downloads = new String[2];
	
	static 
	{
		synonyms = new Hashtable<String, String>();
		synonyms.put("Stratford_(Regional)_station", "Stratford_station");
		synonyms.put("Lewisham_DLR_station", "Lewisham_station");
		synonyms.put("Woolwich_Arsenal_railway_station", "Woolwich_Arsenal_station");
		
		downloads[0] = "http://en.wikipedia.org/wiki/List_of_London_Underground_stations";
		downloads[1] = "http://en.wikipedia.org/wiki/List_of_Docklands_Light_Railway_stations";
	}


	/** Construct without references to graphDb storage */
	public StationProcessor() 
	{
	}

	public StationProcessor(GraphDatabaseService db, Index<Node> index) {
		this();
		this.graphDb = db;
		this.nodeIndex = index;
	}

	protected static String createDisplayName(String name) 
	{
		String s = StringUtils.remove(name, " station").trim();
		s = StringUtils.remove(s, " DLR").trim();
		s = StringUtils.remove(s, " tube").trim();
		s = StringUtils.remove(s, "(Circle, District and Hammersmith & City lines)").trim();
		return s;
	}

	/** Convert the references to stations into a consistent form */
	protected static String makeReference(String link)
	{
		// ignore broken and unpopulated links
		if (StringUtils.isEmpty(link)) return null;
		if (link.endsWith("&action=edit&redlink=1")) return null;
		String s = StringUtils.stripStart(link, defaultPrefix).trim();
		if (s.contains("#")) s = StringUtils.substringBeforeLast(s, "#").trim();
		
		String synonym = synonyms.get(s);
		if (synonym != null) { 
			System.out.println("Replacing " + s + " with " + synonym);
			s = synonym;
		}
		return s;
	}

	// this hack is for complex interchange stations like Earl's Court 
	protected static void processDoubleInOut(String stationRef, Vector<Connections> results, int i, Elements tableRowElements)
	{
		Connections connections = new Connections(stationRef);
		System.out.println("3 consecutive size 1 TDs - likely to be connections");

		Elements dataItems = tableRowElements.get(i).select("td");
		//System.out.println(">>" + dataItems.html());

		Connections lastEntered = results.lastElement();
		if (lastEntered == null) return;
		connections.line = lastEntered.line;

		Elements divItems = dataItems.select("div");
		if (divItems == null || divItems.size() == 0) return;

		Elements anchor = divItems.get(0).select("a");
		String reference = makeReference(anchor.get(0).attr("href"));
		if (connections.preceding == null) connections.preceding = reference;

		// next row
		Elements destCell = tableRowElements.get(i+1).select("td");
		System.out.println("row +1 >>" + destCell.html());

		Elements destAnchor = destCell.get(0).select("a");
		String destRef = makeReference(destAnchor.get(0).attr("href"));
		connections.next = destRef;

		if (connections.isValid()) results.add(connections);

		// row +2 is same destination, but different source
		Elements sourceCell = tableRowElements.get(i+2).select("td");
		System.out.println("row +2 >>" + sourceCell.html());
		Elements sourceAnchor = sourceCell.get(0).select("a");
		String sourceRef = makeReference(sourceAnchor.get(0).attr("href"));
		Connections secondLink = new Connections(stationRef, lastEntered.line, sourceRef, destRef);
		if (secondLink.isValid()) results.add(secondLink);
		i+=2;
	}


	/** Process the succession-box element to obtain the connections between this station and others */
	protected static Vector<Connections> processSuccessionBox(String stationReference, Document doc) throws IOException
	{
		Elements tableElements = doc.getElementsByClass("succession-box");

		// some pages are inconsistent and put connections in a wikitable rather than a succession box
		if (tableElements == null || tableElements.size() == 0) {
			tableElements = doc.select(".wikitable :contains(Preceding station)");
		}

		System.out.println("Data tables found = " + tableElements.size());

		if (tableElements == null || tableElements.size() == 0) {
			System.out.println("WARNING: no connections found within " + doc.getElementById("firstHeading").text());
			System.exit(1);
		}

		/* for (int n=0; n<tableElements.size(); n++)
		{
			System.out.println(n + " >> " + tableElements.get(n).html());
		} */

		Elements tableRowElements = tableElements.get(0).select("tr");

		System.out.println("Found Rows = " + tableRowElements.size());

		Vector<Connections> results = new Vector<Connections>();

		for (int i = 0; i < tableRowElements.size(); i++) 
		{
			// determine if it's a header or an info row
			Element row = tableRowElements.get(i);

			Elements headerItems = row.select("th");
			Elements dataItems   = row.select("td");

			if (headerItems.size() > 0 && dataItems.size() == 0)
			{	
				System.out.println("Header Row " + i + " has " + headerItems.size() + " elements");
				for (int j = 0; j < headerItems.size(); j++) 
				{
					// System.out.println("HEADER " + i + "=" + headerItems.get(j).text());

					Elements anchor = headerItems.get(j).select("a");
					if (anchor != null && anchor.size() > 0 && anchor.get(0).attr("href").equalsIgnoreCase(defunctCategory))
					{
						System.out.println("STOPPING - reached defunct railways section");
						return results;
					}
				}
				continue;
			}


			System.out.println("Data Row " + i + " has " + dataItems.size() + " elements");

			Connections connections = new Connections(stationReference);

			if (dataItems.get(0).text().equalsIgnoreCase(terminus))
				connections.preceding = terminus;
			else if (dataItems.get(dataItems.size()-1).text().equalsIgnoreCase(terminus))
				connections.next = terminus;

			// e.g. 2 inward routes e.g Finchley Central, Westferry DLR 
			if (dataItems.size() == 1)
			{
				// Earl's Court is a special case, as it has two outward routes
				if (i+2 < tableRowElements.size())
				{
					if (tableRowElements.get(i+1).select("td").size() == 1 && tableRowElements.get(i+2).select("td").size() == 1) {
						processDoubleInOut(stationReference, results, i, tableRowElements);
						continue;
					}
				}

				Connections lastEntered = results.lastElement();
				if (lastEntered == null) continue;
				Elements divItems = dataItems.select("div");
				if (divItems == null || divItems.size() == 0) continue;

				Elements anchor = divItems.get(0).select("a");
				String reference = makeReference(anchor.get(0).attr("href"));
				System.out.println(divItems.get(0).text() + " => " + reference);
				if (connections.preceding == null) connections.preceding = reference;
				// if the preceding station is the same as the last entry, don't bother entering a duplicate
				if (lastEntered.preceding.equals(reference)) continue;	
				connections.next = lastEntered.next;
				connections.line = lastEntered.line;
				if (connections.isValid()) results.add(connections);
				continue;
			}

			// e.g. 2 in and outward routes e.g. Canning Town and Camden Town
			if (dataItems.size() == 2)
			{
				Connections lastEntered = results.lastElement();
				if (lastEntered == null) break;
				Elements divItems = dataItems.select("div");
				for (int j = 0; j < divItems.size(); j+=2)
				{
					Elements anchor = divItems.get(j).select("a");
					String reference = makeReference(anchor.get(0).attr("href"));
					System.out.println(divItems.get(j).text() + " => " + reference);
					if (connections.preceding == null) connections.preceding = reference;
					else if (connections.next == null) connections.next = reference;
				}	
				connections.line = lastEntered.line;
				if (connections.isValid()) results.add(connections);
				continue;
			}

			// 1 in 2 out e.g. Liverpool Street
			if (dataItems.size() == 4)
			{
				// System.out.println("Processing: " + dataItems.html());

				Connections lastEntered = results.lastElement();
				if (lastEntered == null) break;

				Elements anchor0 = dataItems.select("td").select("a");
				if (anchor0 != null && anchor0.size() > 0) 
					connections.line = makeReference(anchor0.get(0).attr("href"));
				else {
					anchor0 = dataItems.select("a");
					if (anchor0 != null && anchor0.size() > 0) 
						connections.line = makeReference(anchor0.get(0).attr("href"));
				}

				if (connections.line != null)
					System.out.println("LINE = " + connections.line);

				Elements divItems = dataItems.select("div");
				for (int j = 0; j < divItems.size(); j++)
				{
					Elements anchor = divItems.get(j).select("a");
					if (anchor == null || anchor.size() == 0) {
						System.out.println("Is terminus");
					}
					else
					{
						String reference = makeReference(anchor.get(0).attr("href"));
						System.out.println(j + " => " + reference);
						if (connections.next == null) { 
							connections.next = reference;
							break;
						}
					}	
				}
				connections.preceding = lastEntered.preceding;
				if (connections.isValid()) results.add(connections);
				continue;
			}


			// the more conventional case, 5 elements describing 1 in 1 out
			for (int j = 0; j < dataItems.size(); j++) 
			{
				// another stopping condition
				String description = dataItems.get(j).text();
				if (description != null && (description.startsWith(former) || description.startsWith(outOfSystem) || description.startsWith(transfer) || 
						description.startsWith(abandoned) || description.startsWith(disused) || description.startsWith(serviceArrangement) || description.startsWith(historic))) {
					System.out.println("STOPPING - reached " + description + " section");
					return results;
				} 

				// rather than use ambiguous plaintext, stations are referenced by their hyperlink URIs
				Elements anchor = dataItems.get(j).select("a");
				if (anchor != null && anchor.size() > 0)
				{
					String reference = makeReference(anchor.get(0).attr("href"));
					System.out.println(dataItems.get(j).text() + " => " + reference);
					if (StringUtils.isEmpty(reference)) continue;
					if (connections.preceding == null) connections.preceding = reference;
					else if (connections.line == null) connections.line = reference;
					else if (connections.next == null) connections.next = reference;
				}
			}
			// only allow connections where we know line, preceding and next stations
			if (connections.isValid()) results.add(connections);
			System.out.println();
		}
		return results;
	}


	/** This defines how the data in the stored Wikipedia pages will be processed. 
	 *  We'll create the station nodes first, then connect them up, that ensures we'll only create stations 
	 *  listed in our original manifests
	 */
	protected Vector<Connections> processStation(File dataFile, String canonicalLink) throws DomainException
	{
		try
		{
			if (canonicalLink.contains("#")) System.exit(1);
			
			Document doc = Jsoup.parse(dataFile, "UTF-8");
			String stationName = doc.getElementById("firstHeading").text();
			stationName = createDisplayName(stationName);

			// TODO could check against document link to find inconsistencies (e.g. content page for St. John's Wood has no dot)
			// String articleName = doc.getElementById("ca-nstab-main").select("a").attr("href");
			String stationReference = makeReference(canonicalLink);
			System.out.println("Reference: " + stationReference + " => " + stationName);

			if (findStationNode(stationReference) != null) {
				System.out.println("Already have an entry for " + stationReference + " - skipping");
				return null;
			}

			// populate our property list
			Hashtable<StationProperties, String> properties = new Hashtable<StationProperties, String>();
			properties.put(StationProperties.displayName, stationName);
			properties.put(StationProperties.stationReference, stationReference);

			// set geographical location
			String geoData = doc.select(".geo").text();
			String latitude = geoData.substring(0, geoData.indexOf(";")).trim();
			String longitude = geoData.substring(geoData.indexOf(";") + 1).trim();
			properties.put(StationProperties.latitude, latitude);
			properties.put(StationProperties.longitude, longitude);

			// first, we create a node for the station in the graph database
			createStationNode(properties);

			Vector<Connections> lines = processSuccessionBox(stationReference, doc);
			if (lines == null || lines.size() == 0) {
				// this should never happen, it makes no sense to have an isolated station
				throw new DomainException(stationName + " has no connections");
			}
			for (int i = 0; i < lines.size(); i++) {
				System.out.println("LINE " + i + " = "+ lines.get(i).toString());
			}
			System.out.println();
			return lines;
		}
		catch(Exception e) 
		{
			e.printStackTrace();
			System.out.println("ERROR: " + e.getMessage() + " whilst processing " + dataFile.getName());
		}
		return null;
	}


	/** This converts the connections data into relationships between the nodes */
	protected void processConnections(Vector<Connections> connections)
	{
		Iterator<Connections> it = connections.iterator();
		int i = 0;
		while(it.hasNext())
		{
			Connections c = it.next();
			// System.out.println(i + "\t" + c.toString());

			if (c.preceding.equalsIgnoreCase(terminus) || c.next.equalsIgnoreCase(terminus)) { 
				// System.out.println("IGNORE: " + c.station + " is a terminus of " + c.line);
				continue;
			}

			Node thisStation = findStationNode(c.station);
			Node precedingStation = findStationNode(c.preceding);
			if (thisStation == null) {
				System.out.println(" MISSING: " + c.station + " in entry " + c.toString());
				continue;
			}
			if (precedingStation == null) {
				System.out.println(" MISSING: " + c.preceding + " in entry " + c.toString());
				continue;
			}
			connectStations(thisStation, precedingStation, c.line);

			Node nextStation = findStationNode(c.next);
			if (nextStation == null) {
				System.out.println(" MISSING: " + c.next + " in entry " + c.toString());
				continue;
			}
			connectStations(thisStation, nextStation, c.line);
			i++;
		}
	}


	/** This contains the logic for persisting stations */
	public void createStationNode(Hashtable<StationProperties, String> properties)
	{	
		if (graphDb == null) return;
		// begin a Neo4j transaction and create a node, then add properties
		Transaction tx = graphDb.beginTx();
		try
		{
			Node stationNode = graphDb.createNode();
			nodeIndex.add(stationNode, StationProperties.stationReference.name(), properties.get(StationProperties.stationReference));
			Enumeration<StationProperties> keys = properties.keys();
			while (keys.hasMoreElements()) {
				StationProperties key = (StationProperties) keys.nextElement();
				String value = properties.get(key);
				if (StationProperties.latitude.name().equals(key) || StationProperties.longitude.name().equals(key))
					stationNode.setProperty(key.toString(), new Double(value));
				else
					stationNode.setProperty(key.toString(), value);
			}
			tx.success();
			System.out.println("CREATED NODE: " + stationNode.getId() + " (" + stationNode.getProperty(StationProperties.stationReference.name()) + ")");
		}
		finally
		{
			tx.finish();
		}
	}


	/** This creates a persistent relationship between two stations */
	// TODO create one relation per line and you should be able to colour the graph like the real tube map
	public void connectStations(Node to, Node from, String lineName)
	{
		if (graphDb == null) return;
		// TODO check to see if a relation exists for this pair
		Transaction tx = graphDb.beginTx();
		try 
		{
			Relationship relation = to.createRelationshipTo(from, RelationTypes.CONNECTS);
			relation.setProperty("line", lineName);
			tx.success();
		}
		finally
		{
			tx.finish();
		}

	}

	public Node findStationNode(String stationReference)
	{
		if (graphDb == null || nodeIndex == null) return null;
		return nodeIndex.get(StationProperties.stationReference.name(), stationReference).getSingle();
	}

	protected void downloadFile(String targetUrl, File inputFile)
	{
		if (!inputFile.exists()) 
		{
			System.out.println("Fetching local copy of data file " + inputFile.getName());
			try {
				FileUtils.copyURLToFile(new URL(targetUrl), inputFile);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			} 
		}
	}



	/** This reads in a manifest of stations, fetches their Wikipedia page one by one storing them locally 
	 *  If a graph database exists, it will then extract the data from the pages and create a graph */
	public void populate()
	{
		for (int d = 0; d < downloads.length; d++) 
		{
			String fileName = StringUtils.substringAfterLast(downloads[d], "wikipedia.org/wiki/") + ".html";
			File storedFile = new File(dataDirName, fileName);
			downloadFile(downloads[d], storedFile);
			
			// store these in memory (could store in database later)
			Vector<Connections> connections = new Vector<Connections>();

			try 
			{
				System.out.println("Processing local copy of data file " + storedFile.getName());
				Document doc = Jsoup.parse(storedFile, "UTF-8");
				System.out.println("Using document called " + doc.title());

				Elements tableElements = doc.getElementsByClass("wikitable");
				System.out.println("Found " + tableElements.size() + " data table(s)");

				Elements tableHeaderElements = tableElements.get(0).select("tr").get(0).select("th");
				System.out.println("Headers...");
				for (int i = 0; i < tableHeaderElements.size(); i++) {
					System.out.println(tableHeaderElements.get(i).text());
				}
				System.out.println();

				Elements tableRowElements = tableElements.get(0).select(":not(thead) tr");
				System.out.println("Found " + tableRowElements.size() + " stations");

				int limit = tableRowElements.size();
				for (int i = 1; i < limit; i++) 
				{
					Element row = tableRowElements.get(i);

					Elements rowSubject = row.select("th").select("a");
					if (rowSubject == null || rowSubject.size() == 0)
						rowSubject = row.select("td").select("a");
					
					String nextLink = rowSubject.get(0).attr("href");
					System.out.println("\n" + i + " ** " + rowSubject.get(0).text() + " -> " + nextLink);

					if (nextLink.contains("#")) nextLink = StringUtils.substringBeforeLast(nextLink, "#").trim();
					String newUrl = "http://en.wikipedia.org" + nextLink;
					File newFile = new File(dataDirName, nextLink);
					if (newFile.getName().startsWith("File")) continue;
					if (!newFile.exists()) {
						FileUtils.copyURLToFile(new URL(newUrl), newFile);
					}

					// TODO might store these properties in a hashtable for use later
					Elements rowItems = row.select("td");
					for (int j = 0; j < rowItems.size(); j++) {
						System.out.println(rowItems.get(j).text());
					}
					System.out.println();

					// if no graphDb is connected this will run, but graph will not be persisted
					try
					{
						Vector<Connections> lines = processStation(newFile, nextLink);
						if (lines != null) connections.addAll(lines);
					}
					catch(DomainException ex) {
						System.out.println("WARNING: " + ex.getMessage());
						System.exit(1);
					}

				}
			} 
			catch (IOException e) {
				e.printStackTrace();
			}

			// next, process the collected connections
			processConnections(connections);
		}
	}


	public static void main(String[] args) 
	{
		// can be run without a graph database, but won't persist anything
		StationProcessor processor = new StationProcessor();
		processor.populate();
	}
}
