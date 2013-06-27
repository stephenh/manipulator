package foo.bar.baz;

import java.io.File;

public interface ServiceLocatorService {
	/**
	* @see SomeTypeHere.locateServiceLocatorService(java.io.File, java.lang.String, foo.bar.baz.ServiceLocatorService[])
	*/
	public ServiceLocatorService locateServiceLocatorService(File xmlFile, String xmlNode, ServiceLocatorService[] delegates);
	
	/**
	* @see SomeTypeHere.yesYouCanDeclareTheseAbstractInJava()
	*/
	public abstract void yesYouCanDeclareTheseAbstractInJava();
	
	/**
	* @see SomeTypeHere.availableServiceLocatorServices(java.lang.Object, java.lang.String...)
	*/
	ServiceLocatorService[] availableServiceLocatorServices(java.lang.Object whyNot, String... uri); // an existing comment is preserved too.
	
	public static final MutableFoo NOT_REALLY_CONSTANT_SO_THIS_INTERFACE_IS_ACTUALLY_STATEFUL = new MutableFoo();
	
}