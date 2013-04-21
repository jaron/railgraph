package org.memento.railgraph;

/**
 * A dedicated exception class that will escalate problems detected in the domain layer
 * This will allow classes in the service layer to handle domain level exceptions 
 * differently from exceptions in their own layer 
 */
@SuppressWarnings("serial")
public class DomainException extends Exception
{
	public DomainException() {
		super();
	}
	
	public DomainException(String message) {
		super(message);
	}
	
	public DomainException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public DomainException(Throwable cause) {
		super(cause);
	}
}
