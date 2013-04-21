package org.memento.railgraph;

import org.apache.commons.lang3.StringUtils;

public class Connections 
{	
	public String station;   // the station in question
	public String line;      // the line in question 
	public String preceding; // the previous station (geographical direction is arbitrary)
	public String next;	     // the subsequent station (geographical direction is arbitrary)

	public Connections(String station) {
		this.station = station;
	}
	
	public Connections(String station, String line, String preceding, String next) 
	{
		this.station = station;
		this.line = line;
		this.preceding = preceding;
		this.next = next;
	}
	
	public boolean isValid() {
		return (!StringUtils.isEmpty(station) && !StringUtils.isEmpty(line) && !StringUtils.isEmpty(next) && !StringUtils.isEmpty(preceding) && !next.equalsIgnoreCase(preceding));
	}
	
	@Override
	public String toString() {
		return "@ " + station + " - " + line + " : FROM " + preceding + " TO " + next;
	}
}
