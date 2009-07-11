package net.bioclipse.qsar.ui.editors;
import net.bioclipse.qsar.business.IQsarManager;
import net.bioclipse.qsar.descriptor.model.DescriptorCategory;
import net.bioclipse.qsar.descriptor.model.DescriptorModel;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
public class DescriptorContentProvider implements ITreeContentProvider {
        IQsarManager qsar;
        public DescriptorContentProvider() {
        //Get qsarmanager via OSGI
        qsar=net.bioclipse.qsar.init.Activator.getDefault().getQsarManager();
        }
        public Object[] getChildren(Object parentElement) {
                if (parentElement instanceof DescriptorCategory) {
                        DescriptorCategory category = (DescriptorCategory) parentElement;
                        if (category.getDescriptors()!=null)
                                return category.getDescriptors().toArray();/*qsar.getDescriptorsInCategory(category).toArray();*/
                }
                return new Object[0];
        }
        public Object getParent(Object element) {
                return null;
        }
        public boolean hasChildren(Object element) {
                return getChildren(element).length>0 ? true : false;
        }
        public Object[] getElements(Object inputElement) {
                if (inputElement instanceof PendingObject) {
                        PendingObject po = (PendingObject) inputElement;
                        return new Object[]{po};
                }
                if (!(inputElement instanceof DescriptorModel)) {
                        return new Object[0];
                }
                return qsar.getFullCategories().toArray();
        }
        public void dispose() {
        }
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
}
