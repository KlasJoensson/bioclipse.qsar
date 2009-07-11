/*******************************************************************************
 * Copyright (c) 2008 The Bioclipse Project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Ola Spjuth
 *     
 ******************************************************************************/
package net.bioclipse.qsar.ui.editors;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.bioclipse.cdk.business.Activator;
import net.bioclipse.cdk.business.ICDKManager;
import net.bioclipse.cdk.domain.ICDKMolecule;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.qsar.QsarFactory;
import net.bioclipse.qsar.QsarPackage;
import net.bioclipse.qsar.QsarType;
import net.bioclipse.qsar.ResourceType;
import net.bioclipse.qsar.ResponseType;
import net.bioclipse.qsar.StructurelistType;
import net.bioclipse.qsar.business.IQsarManager;
import net.bioclipse.ui.dialogs.WSFileDialog;

import org.apache.log4j.Logger;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.ui.viewer.IViewerProvider;
import org.eclipse.emf.databinding.edit.EMFEditObservables;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableMapLabelProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;


/**
 * Page for adding molecular content to model
 * @author ola
 *
 */
public class MoleculesPage extends FormPage implements IEditingDomainProvider, IViewerProvider, IPageChangedListener{

    private TableViewer molViewer;
    private Table molTable;

    private TableViewer preTableViewer;
    private Table preTable;

    private static final Logger logger = Logger.getLogger(MoleculesPage.class);

    ICDKManager cdk;
    IQsarManager qsar;
    DecimalFormat formatter;

    private QsarEditorSelectionProvider selectionProvider;
    private EditingDomain editingDomain;

    private StructurelistType structList;
    private QsarType qsarModel;
    private IProject activeProject;


    public MoleculesPage(FormEditor editor, QsarType qsarModel, 
                         EditingDomain editingDomain, QsarEditorSelectionProvider selectionProvider) {

        super(editor, "qsar.molecules", "Molecules");
        this.qsarModel=qsarModel;
        this.editingDomain=editingDomain;

        //Get managers
        cdk=Activator.getDefault().getCDKManager();
        qsar=net.bioclipse.qsar.init.Activator.getDefault().getQsarManager();

        //Set up formatter
        formatter = new DecimalFormat("0.00");

        this.selectionProvider=selectionProvider;

        //Get mollist from qsar model, init if empty (should not be)
        structList=qsarModel.getStructurelist();
        if (structList==null){
            structList=QsarFactory.eINSTANCE.createStructurelistType();
            qsarModel.setStructurelist( structList);
        }

        editor.addPageChangedListener(this);
        if (editor.getEditorInput() instanceof IFileEditorInput) {
            IFileEditorInput fin = (IFileEditorInput) editor.getEditorInput();
            activeProject=fin.getFile().getProject();
        }

    }


    /**
     * Add content to form
     */
    @Override
    protected void createFormContent(IManagedForm managedForm) {


        ScrolledForm form = managedForm.getForm();
        FormToolkit toolkit = managedForm.getToolkit();
        form.setText("Molecules for QSAR analysis");
        toolkit.decorateFormHeading(form.getForm());

        IProject project=((QsarEditor)getEditor()).getActiveProject();
        ToolbarHelper.setupToolbar(form, project);

        //        form.setBackgroundImage(FormArticlePlugin.getDefault().getImage(FormArticlePlugin.IMG_FORM_BG));
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        form.getBody().setLayout(layout);

        createMoleculesSection(form, toolkit);
        populateMolsViewerFromModel();

        createPreprocessingSection(form, toolkit);
        //        populatePreViewerFromModel();  //TODO!

        preTableViewer.getTable().setEnabled(false); //TODO: change!

        addDragAndDrop();

        //Post selections to Eclipse via our intermediate selectionprovider
        selectionProvider.setSelectionProviderDelegate( molViewer );


    }

    private void addDragAndDrop() {
        int ops = DND.DROP_COPY | DND.DROP_MOVE;
        Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer(), FileTransfer.getInstance()};
        molViewer.addDropSupport(ops, transfers, new ViewerDropAdapter(molViewer){

            @Override
            public boolean performDrop(Object data) {

                if (!((data instanceof String[]) || (data instanceof IStructuredSelection))) {
                    return false;
                }

                final Object indata=data;

                WorkspaceJob job=new WorkspaceJob("Adding resources to QSAR project"){

                    @Override
                    public IStatus runInWorkspace(IProgressMonitor monitor)
                    throws CoreException {

                        List<IResource> resources=new ArrayList<IResource>();

                        //Handle selections within Bioclipse
                        if (indata instanceof String[]){

                            List<IResource> newRes=handleDropOfFiles((String[])indata, monitor);
                            if (newRes!=null && newRes.size()>0)
                                resources.addAll(newRes);
                        }

                        //Handle selections within Bioclipse
                        else if (indata instanceof IStructuredSelection){

                            IStructuredSelection ssel = (IStructuredSelection) indata;
                            for (Object obj : ssel.toList()){
                                if (obj instanceof IResource) {
                                    IResource res = (IResource) obj;
                                    resources.add(res);
                                }
                            }
                        }

                        //If none, return error
                        if (resources.size()<=0) return Status.CANCEL_STATUS;

                        //Add resources to model and molecules folder is necessary
                        try{
                            addResources(resources.toArray(new IResource[0]), monitor);
                        }catch (final UnsupportedOperationException e){
                            Display.getDefault().syncExec(new Runnable(){
                                public void run() {
                                    showError("Error adding files: " + e.getMessage());
                                }
                            });
                        }

                        Display.getDefault().syncExec(new Runnable(){

                            public void run() {
                                molViewer.getTable().setFocus();
                            }

                        });
                        return Status.OK_STATUS;

                    }

                };

                job.setUser(true);
                job.schedule();

                return true;

            }

            @Override
            public boolean validateDrop(Object target, int operation,
                                        TransferData transferType) {
                return true;
            }});
    }


    /**
     * Handle the dropping of files on molviewer.
     * Test if contains molecules and copy to molecules folder in that case.
     * @param data
     * @param monitor
     * @return
     */
    protected List<IResource> handleDropOfFiles(final String[] data, IProgressMonitor monitor) {

        final List<IResource> retlist=new ArrayList<IResource>();

        for (String path : (String[])data){

            //For now, removed parsing of mols on addition
            
//            try {
//                List<ICDKMolecule> lst = cdk.loadMolecules(path);
//                if (lst!=null && lst.size()>0){

                    //Copy to molecules folder
                    IResource res=copyFileToMoleculesFolder(path, monitor);
                    if (res!=null)
                        retlist.add(res);
//                }
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (BioclipseException e) {
//                e.printStackTrace();
//            } catch (CoreException e) {
//                e.printStackTrace();
//            }
        }

        return retlist;
    }


    /**
     * Copy the file into the molecules folder
     * @param path absolut path to the file
     * @param monitor
     * @return IFile in molecules folder
     */
    protected IResource copyFileToMoleculesFolder(String path, IProgressMonitor monitor) {

        java.io.File file=new java.io.File(path);
        final String filename=file.getName();

        FileInputStream instream;
        IFile newfile=null;
        try {
            instream = new FileInputStream(file);

            IFolder molfolder=activeProject.getFolder("molecules");
            IPath newpath=molfolder.getProjectRelativePath().append(filename);

            newfile=activeProject.getFile(newpath);

            if (newfile.exists()){

                final boolean[] valueIsSet = {false};
                final boolean[] answer     = {false};

                Display.getDefault().asyncExec( new Runnable() {
                    public void run() {
                        synchronized ( valueIsSet ) {
                            answer[0] = MessageDialog.openQuestion(
                                                                   getSite().getShell(), 
                                                                   "Overwrite file?", 
                                                                   "File " + filename + "exists in " 
                                                                   + "project folder 'molecules'. " 
                                                                   + "Would you like to replace "
                                                                   + "it?" );
                            valueIsSet[0] = true;
                            valueIsSet.notifyAll();
                        }
                    }
                } );

                synchronized ( valueIsSet ) {
                    while ( !valueIsSet[0] ) {
                        try {
                            valueIsSet.wait();
                        } 
                        catch ( InterruptedException e ) {
                            continue;
                        }
                    }
                }

                if (!answer[0]) return null;

                newfile.setContents(instream, true,false, monitor);

            }else{
                newfile.create(instream, true, monitor);

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (CoreException e) {
            e.printStackTrace();
            return null;
        }

        return newfile;

    }


    /**
     * Add resources to QSAR model. First check if contains molecules. 
     * Next copy the resource into the project/molecules folder if not already
     * there.
     * @param resources
     * @param monitor
     * @return
     */
    protected void addResources(final IResource[] resources, final IProgressMonitor monitor) {

        List<IResource> resourcesToAdd=new ArrayList<IResource>();
        
        //Copy files to project if needed
        for (IResource resource : resources){

            if (resource instanceof IFile) {
                IFile file = (IFile) resource;
                boolean skipFile=false;

                //Check if this file is already in model
                for (ResourceType existingRes : structList.getResources()){
                    if (existingRes.getName().equals(file.getName())){
                        throw new UnsupportedOperationException("File: " + 
                                                                file.getName() + 
                        " already exists in QSAR analysis.");
                    }
                }

                try {
                    //Verify this is a file with at least one molecule
                    List<ICDKMolecule> mollist = cdk.loadMolecules(file);
                    if (mollist!=null && mollist.size()>0){

                        //If resource is in another project,
                        //copy it to molecules folder as use that copy
                        if (file.getProject()!=activeProject){
                            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                            IPath projectPath = activeProject.getFullPath(),
                            molFolderPath = projectPath.append("molecules"),
                            destinationPath=molFolderPath.append(file.getName());

                            final IFile newfile=root.getFile(destinationPath);
                            if (newfile.exists()){
                                final String filename=file.getName();

                                ConfirmRunnable cr=new ConfirmRunnable(getSite().getShell(),
                                                                       "Overwrite file?", 
                                                                       "File " + filename + "exists in QSAR project, " +
                                                                       "folder 'molecules'. " +
                                "Would you like to replace it?");

                                Display.getDefault().syncExec(cr);

                                if (!cr.getAnswer()) skipFile=true;

                                else{
                                    try {
                                        newfile.setContents(file.getContents(), true, true, monitor);
                                    } catch (CoreException e) {
                                        e.printStackTrace();
                                    }
                                }



                            }else{
                                //Copy it
                                file.copy(destinationPath, true, monitor);
                            }

                            file=root.getFile(destinationPath);
                        }


                        if (!skipFile){
                            resourcesToAdd.add( resource );
                        }
                    }
                } catch (final Exception e) {
                    Display.getDefault().syncExec(new Runnable(){
                        public void run() {
                            showError("Could not add molecule file. \n\nReason: " + e.getMessage());
                        }
                    });
                }
            }
        }
        
        //Ok, add these resources to QsarModel using manager

        try {
            qsar.addResourcesToQsarModel( qsarModel, editingDomain, resourcesToAdd, monitor );
        } catch ( IOException e ) {
            logger.error(e.getStackTrace());
            showError( e.getMessage() );
        } catch ( BioclipseException e ) {
            logger.error(e.getStackTrace());
            showError( e.getMessage() );
        } catch ( CoreException e ) {
            logger.error(e.getStackTrace());
            showError( e.getMessage() );
        }

    }


    private void populateMolsViewerFromModel() {

        // The content provider is responsible to handle add and
        // remove notification for the Person#address EList
        ObservableListContentProvider provider = new ObservableListContentProvider();
        molViewer.setContentProvider(provider);

        // The label provider in turn handles the addresses
        // The EStructuralFeature[] defines which fields get shown
        // in the TableViewer columns
        IObservableSet knownElements = provider.getKnownElements();
        IObservableMap[] observeMaps = EMFEditObservables.
        observeMaps(editingDomain, knownElements, new EStructuralFeature[]{
                QsarPackage.Literals.RESOURCE_TYPE__NAME,
                QsarPackage.Literals.RESOURCE_TYPE__NO_MOLS,
                QsarPackage.Literals.RESOURCE_TYPE__NO2D,
                QsarPackage.Literals.RESOURCE_TYPE__NO3D});
        ObservableMapLabelProvider labelProvider =
            new ObservableQSARLabelProvider(observeMaps);
        molViewer.setLabelProvider(labelProvider);

        // Person#addresses is the Viewer's input
        molViewer.setInput(EMFEditObservables.observeList(Realm.getDefault(), editingDomain, structList,
                                                          QsarPackage.Literals.STRUCTURELIST_TYPE__RESOURCES));

    }


    private void createMoleculesSection(final ScrolledForm form, FormToolkit toolkit) {


        Section molSection =
            toolkit.createSection(
                                  form.getBody(),
                                  Section.TWISTIE | Section.DESCRIPTION);
        molSection.setActiveToggleColor(
                                        toolkit.getHyperlinkGroup().getActiveForeground());
        molSection.setToggleColor(
                                  toolkit.getColors().getColor(IFormColors.SEPARATOR));
        toolkit.createCompositeSeparator(molSection);
        Composite client = toolkit.createComposite(molSection, SWT.WRAP);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        client.setLayout(layout);

        molViewer = new TableViewer(client, SWT.BORDER | SWT.MULTI);
        molTable=molViewer.getTable();
        toolkit.adapt(molTable, true, true);
        GridData gd=new GridData(GridData.FILL_VERTICAL);
        gd.widthHint=350;
        gd.verticalSpan=2;
        molTable.setLayoutData( gd );

        molTable.setHeaderVisible(true);
        //          molTable.setLinesVisible(true);
        toolkit.adapt(molTable, true, true);

        //Add name columns
        TableLayout tableLayout = new TableLayout();
        molTable.setLayout(tableLayout);
        TableViewerColumn ixcol=new TableViewerColumn(molViewer,SWT.BORDER);
        ixcol.getColumn().setText("Name");
        tableLayout.addColumnData(new ColumnPixelData(175));

        //Add # column
        TableViewerColumn col=new TableViewerColumn(molViewer,SWT.BORDER);
        col.getColumn().setText("# Molecules");
        tableLayout.addColumnData(new ColumnPixelData(75));

        //Add 2D column
        TableViewerColumn col2d=new TableViewerColumn(molViewer,SWT.BORDER);
        col2d.getColumn().setText("2D");
        tableLayout.addColumnData(new ColumnPixelData(30));

        //Add 2D column
        TableViewerColumn col3d=new TableViewerColumn(molViewer,SWT.BORDER);
        col3d.getColumn().setText("3D");
        tableLayout.addColumnData(new ColumnPixelData(30));


        molTable.addKeyListener( new KeyListener(){
            public void keyPressed( KeyEvent e ) {
                //Delete key
                if (e.keyCode==SWT.DEL){
                    deleteSelectedMolecules();
                }

                //Space key, toggle selection
                if (e.keyCode==32){

                    IStructuredSelection msel=(IStructuredSelection) molViewer.getSelection();
                    //TODO: implement

                }
            }
            public void keyReleased( KeyEvent e ) {
            }
        });

        //If focus gained, make this viewer provide selections
        molViewer.getTable().addFocusListener(new FocusListener(){

            public void focusGained(FocusEvent e) {
                molViewer.setSelection(null);
                selectionProvider.setSelectionProviderDelegate( molViewer );
            }

            public void focusLost(FocusEvent e) {
            }
        });


        Button btnAdd=toolkit.createButton(client, "Add...", SWT.PUSH);
        btnAdd.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                addMoleculeFile();
            }
        });
        GridData gd2=new GridData();
        gd2.verticalAlignment=SWT.BEGINNING;
        gd2.widthHint=60;
        btnAdd.setLayoutData( gd2 );

        Button btnDel=toolkit.createButton(client, "Remove", SWT.PUSH);
        btnDel.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                deleteSelectedMolecules();
            }
        });
        gd2=new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd2.widthHint=60;
        btnDel.setLayoutData( gd2 );


        //Wrap up section
        toolkit.paintBordersFor(client);
        molSection.setText("Molecules");
        molSection.setDescription("Molecules for descriptor calculations");
        molSection.setClient(client);
        molSection.setExpanded(true);
        molSection.addExpansionListener(new ExpansionAdapter() {
            public void expansionStateChanged(ExpansionEvent e) {
                form.reflow(false);
            }
        });

        gd = new GridData(GridData.FILL_BOTH);
        molSection.setLayoutData(gd);        		



    }


    private void createPreprocessingSection( final ScrolledForm form,
                                             FormToolkit toolkit) {

        Section preSection =
            toolkit.createSection(
                                  form.getBody(),
                                  Section.TWISTIE | Section.DESCRIPTION);
        preSection.setActiveToggleColor(
                                        toolkit.getHyperlinkGroup().getActiveForeground());
        preSection.setToggleColor(
                                  toolkit.getColors().getColor(IFormColors.SEPARATOR));
        toolkit.createCompositeSeparator(preSection);
        Composite client = toolkit.createComposite(preSection, SWT.WRAP);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        client.setLayout(layout);


        //Query TreeViewer
        preTableViewer = new TableViewer (client, SWT.BORDER | SWT.SINGLE);
        preTableViewer.setContentProvider( new PreprocessingContentProvider() );
        preTableViewer.setLabelProvider( new PreprocessingLabelProvider() );
        preTable=preTableViewer.getTable();
        toolkit.adapt(preTable, true, true);
        GridData gd6=new GridData(GridData.FILL_VERTICAL);
        gd6.widthHint=200;
        gd6.verticalSpan=4;
        preTable.setLayoutData( gd6 );

        preTable.addKeyListener( new KeyListener(){
            public void keyPressed( KeyEvent e ) {

                //Delete key
                if (e.keyCode==SWT.DEL){
                    deletePreprocessingStep();
                }

            }
            public void keyReleased( KeyEvent e ) {
            }
        });


        Button btnAdd=toolkit.createButton(client, "Add...", SWT.PUSH);
        btnAdd.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                addPreprocessingStep();
            }
        });
        GridData gd2=new GridData();
        gd2.verticalAlignment=SWT.BEGINNING;
        gd2.widthHint=60;
        btnAdd.setLayoutData( gd2 );

        Button btnDel=toolkit.createButton(client, "Remove", SWT.PUSH);
        btnDel.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                deletePreprocessingStep();
            }
        });
        gd2=new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd2.widthHint=60;
        btnDel.setLayoutData( gd2 );

        Button btnUp=toolkit.createButton(client, "Up", SWT.PUSH);
        btnUp.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                moveSelectedUp();
            }
        });
        gd2=new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd2.widthHint=60;
        btnUp.setLayoutData( gd2 );

        Button btnDown=toolkit.createButton(client, "Down", SWT.PUSH);
        btnDown.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                moveSelectedDown();
            }
        });
        gd2=new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd2.widthHint=60;
        btnDown.setLayoutData( gd2 );


        //Wrap up section
        toolkit.paintBordersFor(client);
        preSection.setText("Molecule preprocessing");
        preSection.setDescription("Add/remove and order preprocessing steps");
        preSection.setClient(client);
        preSection.setExpanded(true);
        preSection.addExpansionListener(new ExpansionAdapter() {
            public void expansionStateChanged(ExpansionEvent e) {
                form.reflow(false);
            }
        });
        GridData gd = new GridData(GridData.FILL_BOTH);
        preSection.setLayoutData(gd);        

        //Post selections to Eclipse
        //          getSite().setSelectionProvider(queryViewer);

    }

    protected void moveSelectedDown() {

        showMessage("Not implemented");
        //TODO: implement

    }


    protected void moveSelectedUp() {

        showMessage("Not implemented");
        //TODO: implement

    }


    /**
     * After click of "ADD" button, add preprocess step
     */
    protected void addPreprocessingStep() {

        showMessage("Not implemented");
        //TODO: implement

    }

    protected void deletePreprocessingStep() {

        showMessage("Not implemented");
        //TODO: implement

        /*
        IStructuredSelection sel=(IStructuredSelection)preTableViewer.getSelection();
        for (Object obj : sel.toList()){

            }
        }

        preViewe 
        preTableViewer.getTable().setFocus();
         */
    }



    protected void changeMolViewerState( Object obj, boolean newState ) {

        showMessage("Not implemented");
        //TODO: implement


        /*
        molViewer.setChecked( obj, newState );

        if ( obj instanceof MoleculeResource ) {
            molViewer.expandToLevel(obj, 1);
            MoleculeFile cont = (MoleculeFile) obj;
            for (PcoreMolecule mol : cont.getChildren()){
                molViewer.setChecked( mol, newState );
                for (PcoreConformer conf : mol.getConformers()){
                    molViewer.setChecked( conf, newState );
                }
            }
        }
        if ( obj instanceof PcoreMolecule ) {
//            molViewer.expandToLevel(obj, AbstractTreeViewer.ALL_LEVELS);
            PcoreMolecule pmol=(PcoreMolecule)obj;
            for (PcoreConformer conf : pmol.getConformers()){
                molViewer.setChecked( conf, newState );
            }
        }
         */
    }

    /**
     * Handle the case when users press the ADD button next to moleculeviewer
     */
    protected void addMoleculeFile() {

        IProject proj=((QsarEditor)getEditor()).getActiveProject();

        WSFileDialog dlg=new WSFileDialog(getEditorSite().getShell()
                                          ,SWT.MULTI,"Select molecules"
                                          ,proj,true,null,null);

        //Collect a list of resources currently in viewer 
        //to hide them in dialog
        //TODO


        //		if (molecules!=null && molecules.size()>0){
        //			List<IResource> res=new ArrayList<IResource>();
        //			for (MoleculeResource r : molecules){
        //				res.add(r.getResource());
        //			}
        //			dlg.setBlacklistFilter( res );
        //		}

        int r=dlg.open();
        if (r==Window.CANCEL){
            return;
        }

        if (dlg.getMultiResult()==null || dlg.getMultiResult().length<=0){
            showMessage("Please select at least one molecule to add");
            return;
        }

        addResources(dlg.getMultiResult(), new NullProgressMonitor());

        //		for (IResource resource : dlg.getMultiResult()){
        //
        //			//Also add to QSAR model
        //			MoleculeResourceType mol1=QsarFactory.eINSTANCE.createMoleculeResourceType();
        //			mol1.setId(resource.getName());
        //			mol1.setName(resource.getName());
        //			mol1.setFile(resource.getFullPath().toString());
        //			Command cmd=AddCommand.create(editingDomain, moleculeList, 
        //					QsarPackage.Literals.MOLECULELIST_TYPE__MOLECULE_RESOURCE, mol1);
        //
        //			//Execute the CompoundCommand
        //			editingDomain.getCommandStack().execute(cmd); 		
        //		}

        molViewer.refresh();

    }



    /**
     * Handle the case when users press the Remove button next to moleculeviewer
     * or presses the delete button on something
     */
    protected void deleteSelectedMolecules() {

        IStructuredSelection ssel=(IStructuredSelection) molViewer.getSelection();
        if (ssel == null) {
            showMessage("Please select a molecule to remove");
            return;
        }

        qsar.removeResourcesFromModel(qsarModel, editingDomain, ssel.toList());

        molViewer.refresh();
    }

    
    private void showMessage(String message) {
        MessageDialog.openInformation( getSite().getShell(),
                                       "Information",
                                       message );
    }

    private void showError(String message) {
        MessageDialog.openError( getSite().getShell(),
                                 "Information",
                                 message );
    }


    public void activatePage() {

        selectionProvider.setSelectionProviderDelegate( molViewer );
    }

    public class Stopwatch {
        private long start;
        private long stop;

        public void start() {
            start = System.currentTimeMillis(); // start timing
        }

        public void stop() {
            stop = System.currentTimeMillis(); // stop timing
        }

        public long elapsedTimeMillis() {
            return stop - start;
        }

        //return number of seconds
        public String toString() {
            return "" + Long.toString(elapsedTimeMillis()/1000); // print execution time
        }
    }

    public EditingDomain getEditingDomain() {
        return editingDomain;
    }


    public Viewer getViewer() {
        return molViewer;
    }

    public void pageChanged(PageChangedEvent event) {

        if (event.getSelectedPage()!=this) return;

        activatePage();

    }

}
