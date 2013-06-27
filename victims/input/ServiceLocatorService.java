package foo.bar.baz;

import java.io.File;

public interface ServiceLocatorService {
	public ServiceLocatorService locateServiceLocatorService(File xmlFile, String xmlNode, ServiceLocatorService[] delegates);
	
	public abstract void yesYouCanDeclareTheseAbstractInJava();
	
	ServiceLocatorService[] availableServiceLocatorServices(java.lang.Object whyNot, String... uri); // an existing comment is preserved too.
	
	public static final MutableFoo NOT_REALLY_CONSTANT_SO_THIS_INTERFACE_IS_ACTUALLY_STATEFUL = new MutableFoo();
	
}