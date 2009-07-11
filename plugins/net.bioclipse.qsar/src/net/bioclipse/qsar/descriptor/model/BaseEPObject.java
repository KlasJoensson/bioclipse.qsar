package net.bioclipse.qsar.descriptor.model;

import net.bioclipse.core.domain.BioObject;
import net.bioclipse.qsar.init.Activator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertySource;


public class BaseEPObject extends BioObject{

	String id;
	String name;
	String icon_path;
	Image icon;
	String namesapce;
	
	public String getNamesapce() {
		return namesapce;
	}
	public void setNamesapce(String namesapce) {
		this.namesapce = namesapce;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getIcon_path() {
		return icon_path;
	}
	public void setIcon_path(String icon_path) {
		this.icon_path = icon_path;
	}
	public Image getIcon() {
		if (icon_path==null){
			return null;
		}
		if (icon==null){
			ImageDescriptor idesc=Activator.getImageDescriptor(icon_path);
			if (idesc==null) return null;

			icon=idesc.createImage();
		}
		return icon;
	}
	public void setIcon(Image icon) {
		this.icon = icon;
	}

	
	public BaseEPObject(String id, String name) {
		this.id=id;
		this.name=name;
	}
	public BaseEPObject(String id, String name, String icon_path) {
		this(id,name);
		this.icon_path=icon_path;
	}

	public Object getAdapter(Class adapter) {

		if (IPropertySource.class.equals(adapter)) {
			return new BaseEPObjectPropertySource(this);
		}

		return super.getAdapter(adapter);
	}
	
	BaseEPObject(){
	}

}
