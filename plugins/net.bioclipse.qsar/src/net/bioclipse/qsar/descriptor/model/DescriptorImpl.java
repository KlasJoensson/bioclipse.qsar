package net.bioclipse.qsar.descriptor.model;

import java.util.List;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertySource;

public class DescriptorImpl extends BaseEPObject{

	private DescriptorProvider provider;
	private boolean requires3D;
	private List<DescriptorParameter> parameters;
	private String definition;
	private String description;

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDefinition() {
		return definition;
	}
	public void setDefinition(String definition) {
		this.definition = definition;
	}
	public List<DescriptorParameter> getParameters() {
		return parameters;
	}
	public void setParameters(List<DescriptorParameter> parameters) {
		this.parameters = parameters;
	}
	public boolean isRequires3D() {
		return requires3D;
	}
	public void setRequires3D(boolean requires3D) {
		this.requires3D = requires3D;
	}
	public DescriptorProvider getProvider() {
		return provider;
	}
	public void setProvider(DescriptorProvider provider) {
		this.provider = provider;
	}
	public DescriptorImpl(String id, String name) {
		super(id, name);
	}
	public DescriptorImpl(String id, String name, String icon_path) {
		super(id, name, icon_path);
	}

	DescriptorImpl(){
	}


	public Object getAdapter(Class adapter) {

		if (IPropertySource.class.equals(adapter)) {
			return new DescriptorImplPropertySource(this);
		}

		return super.getAdapter(adapter);
	}
	
	
}
