package net.bioclipse.qsar.descriptor.model;

import net.bioclipse.qsar.business.IQsarManager;
import net.bioclipse.qsar.init.Activator;

import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

public class DescriptorProviderPropertySource extends BaseEPObjectPropertySource{

    protected static final String PROPERTY_TYPE = "Type";
    protected static final String PROPERTY_NO_DESCR = "# descriptors";

    IQsarManager qsar;
    
	public DescriptorProviderPropertySource(BaseEPObject item) {
		super(item);
		qsar=Activator.getDefault().getQsarManager();
	}
	
	@Override
	public void addPropertiesWithValues() {

		//Add id and name
		super.addPropertiesWithValues();
		
		//Add type
        PropertyDescriptor descriptor;
        descriptor = new TextPropertyDescriptor(PROPERTY_TYPE,"Type");
        descriptor.setCategory("General");
        getProperties().add(descriptor);
    	//Add type value
        getValueMap().put(PROPERTY_TYPE,"Provider");

		//Add type
        PropertyDescriptor descriptor2;
        descriptor2 = new TextPropertyDescriptor(PROPERTY_NO_DESCR,"# descriptors");
        descriptor2.setCategory("General");
        getProperties().add(descriptor2);
    	//Add type value
        getValueMap().put(PROPERTY_NO_DESCR,qsar.getDescriptors((DescriptorProvider)getItem()).size());

	}

}
