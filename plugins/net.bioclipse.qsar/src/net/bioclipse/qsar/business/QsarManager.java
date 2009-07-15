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
package net.bioclipse.qsar.business;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import net.bioclipse.cdk.business.ICDKManager;
import net.bioclipse.cdk.domain.ICDKMolecule;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.domain.IMolecule;
import net.bioclipse.inchi.business.IInChIManager;
import net.bioclipse.qsar.DescriptorType;
import net.bioclipse.qsar.DescriptorlistType;
import net.bioclipse.qsar.DescriptorproviderType;
import net.bioclipse.qsar.DescriptorresultType;
import net.bioclipse.qsar.ParameterType;
import net.bioclipse.qsar.QSARConstants;
import net.bioclipse.qsar.QsarFactory;
import net.bioclipse.qsar.QsarPackage;
import net.bioclipse.qsar.QsarType;
import net.bioclipse.qsar.ResourceType;
import net.bioclipse.qsar.ResponseType;
import net.bioclipse.qsar.ResponseunitType;
import net.bioclipse.qsar.StructureType;
import net.bioclipse.qsar.StructurelistType;
import net.bioclipse.qsar.descriptor.IDescriptorCalculator;
import net.bioclipse.qsar.descriptor.IDescriptorResult;
import net.bioclipse.qsar.descriptor.model.Descriptor;
import net.bioclipse.qsar.descriptor.model.DescriptorCategory;
import net.bioclipse.qsar.descriptor.model.DescriptorImpl;
import net.bioclipse.qsar.descriptor.model.DescriptorModel;
import net.bioclipse.qsar.descriptor.model.DescriptorParameter;
import net.bioclipse.qsar.descriptor.model.DescriptorProvider;
import net.bioclipse.qsar.descriptor.model.ResponseUnit;
import net.bioclipse.qsar.init.Activator;
import net.bioclipse.qsar.prefs.QSARPreferenceInitializer;
import net.bioclipse.qsar.prefs.QsarPreferenceHelper;
import net.bioclipse.qsar.util.QsarAdapterFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;

public class QsarManager implements IQsarManager{

    private static final Logger logger = Logger.getLogger(QsarManager.class);


    //The descriptor model
    DescriptorModel model;

    //For console namespace
    public String getManagerName() {
        return "qsar";
    }



    /*====================================================
     * model initialization from EP below
     * ====================================================
     */

    public DescriptorModel getModel() {
        if (model==null) initializeDescriptorModel();
        return model;
    }


    /**
     * Populate model from OWL Ontology and Extension Point.
     */
    private void initializeDescriptorModel() {

        //Firstly, build hierarchy from descriptor OWL with Jena
        try {
            model=JenaReader.populateHierarchy();
            logger.debug("** descriptor model initialized from ontology **");
        } catch (IOException e) {
            logger.error("Could not initialize Jena model: " + e.getMessage());
            //			e.printStackTrace();
            return;
        } catch (URISyntaxException e) {
            logger.error("Could not initialize Jena model: " + e.getMessage());
            //			e.printStackTrace();
            return;
        }
        if (model==null){
            logger.error("Could not initialize Jena model. ");
            return;
        }

        //Create new list of providers
        model.setProviders(QsarHelper.readProvidersAndDescriptorImplsfromEP());

        //Create new list of providers
        model.setUnits( QsarHelper.readUnitsFromEP());

        //        //Add categories from extension point
        //        addCategories(serviceObjectExtensions, catlist);
        //TODO: might be used to complement categories from ontology at later point


    }



    /*====================================================
     * Getter/setter for model below
     * ====================================================
     */


    /**
     * Get all descriptor categories. Read from EP if not initialized.
     * @return List<String> of category ID's.
     */
    public List<String> getCategories() {

        if (model==null) initializeDescriptorModel();
        List<String> ret=new ArrayList<String>();
        for (DescriptorCategory cat : model.getCategories()){
            ret.add(cat.getId());
        }

        return ret;
    }

    /**
     * 
     * @return
     */
    public List<String> getResponseUnits() {
        if (model==null) initializeDescriptorModel();
        List<String> ret=new ArrayList<String>();
        for (ResponseUnit unit : model.getUnits()){
            ret.add(unit.getId());
        }
        return ret;
    }
    

    /**
     * Get all descriptor providers. Read from EP if not initialized.
     * @return List<String> of provider ID's.
     */
    public List<String> getProviders() {

        if (model==null) initializeDescriptorModel();
        List<String> ret=new ArrayList<String>();
        for (DescriptorProvider prov : model.getProviders()){
            ret.add(prov.getId());
        }

        return ret;
    }

    /**
     * Get all descriptor categories. Read from EP if not initialized.
     * @return List of categories.
     */
    public List<DescriptorCategory> getFullCategories() {
        if (model==null) initializeDescriptorModel();
        return model.getCategories();
    }
    
    /**
     * Get all response unites. Read from EP if not initialized.
     * @return List of categories.
     */
    public List<ResponseUnit> getFullResponseUnits() {
        if (model==null) initializeDescriptorModel();
        return model.getUnits();
    }


    /**
     * Get all descriptor providers. Read from EP if not initialized.
     * @return List of providers.
     */
    public List<DescriptorProvider> getFullProviders() {
        if (model==null) initializeDescriptorModel();
        return model.getProviders();
    }

    /**
     * @param providerID the ID of the provider
     * @return provider or null if not found
     */
    public DescriptorProvider getProviderByID(String providerID) {

        for (DescriptorProvider prov : getFullProviders() ){
            if (prov.getId().equals(providerID)){
                return prov;
            }
        }
        return null;
    }

    /**
     * @param categoryID the ID of the category
     * @return category or null if not found
     */
    public DescriptorCategory getCategoryByID(String categoryID) {
        for (DescriptorCategory cat: getFullCategories() ){
            if (cat.getId().equals(categoryID)){
                return cat;
            }
        }
        return null;
    }

    /**
     * Get all available descriptors.
     * @return List of descriptor IDs or empty List.
     */
    public List<String> getDescriptorIDs() {
        //Collect all descriptors
        List<String> ret=new ArrayList<String>();
        for (Descriptor desc: getFullDescriptors()){
            ret.add(desc.getId());
        }

        return ret;
    }

    public Descriptor getDescriptorByID(String descriptorID) {

        for (Descriptor desc : getFullDescriptors()){
            if (desc.getId().equals(descriptorID))
                return desc;
        }

        return null;
    }


    /**
     * Get all available descriptors.
     * @return List of descriptor IDs or empty List.
     */
    public List<Descriptor> getFullDescriptors() {
        //Collect all descriptors
        List<Descriptor> ret=new ArrayList<Descriptor>();
        for (DescriptorCategory cat : getFullCategories()){
            if (cat.getDescriptors()!=null){
                for (Descriptor desc : cat.getDescriptors()){
                    ret.add(desc);
                }
            }
        }

        //Remove duplicates
        Set<Descriptor> noDups=new LinkedHashSet<Descriptor>(ret);
        List<Descriptor> noDupsList=new ArrayList<Descriptor>(noDups);

        return noDupsList;
    }


    /**
     * Get all descriptors in a category
     * @return List of descriptors or empty List.
     */
    public List<Descriptor> getDescriptorsInCategory(DescriptorCategory category) {
        return category.getDescriptors();
    }

    /**
     * Get all descriptor IDs in a category
     * @return List of descriptor IDs or empty List.
     */
    public List<String> getDescriptorsInCategory(String categoryID) {
        DescriptorCategory category=getCategoryByID(categoryID);

        //Collect all descriptors
        List<String> ret=new ArrayList<String>();
        for (Descriptor desc: category.getDescriptors()){
            ret.add(desc.getId());
        }

        return ret;
    }


    /**
     * Get all descriptor implementation IDs for a provider.
     * @return List of descriptor implementation IDs or empty List.
     */
    public List<String> getDescriptorImplsByProvider(String providerID) {

        DescriptorProvider provider = getProviderByID(providerID);
        List<String> ret=new ArrayList<String>();
        for (DescriptorImpl desc : provider.getDescriptorImpls()){
            ret.add(desc.getId());
        }

        return ret;
    }

    /**
     * Get all descriptor implementations for a provider.
     * @return List of descriptors
     */
    public List<DescriptorImpl> getFullDescriptorImpls(DescriptorProvider provider) {
        return provider.getDescriptorImpls();
    }

    /**
     * Get all descriptor implementations for a provider.
     * @return List of descriptors
     */
    public List<DescriptorImpl> getFullDescriptorImpls() {
        List<DescriptorImpl> ret=new ArrayList<DescriptorImpl>();
        for (DescriptorProvider prov : getFullProviders()){
            if (prov.getDescriptorImpls()!=null)
                ret.addAll(prov.getDescriptorImpls());
        }
        return ret;
    }


    /**
     * Return list of implIDs for a given a descriptorID
     */
    public List<String> getDescriptorImpls(String descriptorID) {
        List<String> ret= new ArrayList<String>();
        for (DescriptorImpl impl : getFullDescriptorImpls()){
            if (impl.getDefinition().equals(descriptorID))
                ret.add(impl.getId());
        }
        return ret;
    }

    /**
     * Return list of descriptorImpls for a given a descriptorID
     */
    public List<DescriptorImpl> getDescriptorImplsForDescriptor(String descriptorID) {
        List<DescriptorImpl> ret= new ArrayList<DescriptorImpl>();
        for (DescriptorImpl impl : getFullDescriptorImpls()){
            if (impl.getDefinition().equals(descriptorID))
                ret.add(impl);
        }
        return ret;
    }


    public DescriptorImpl getDescriptorImplByID(String descriptorImplID) {

        for (DescriptorProvider provider : getFullProviders()){
            for (DescriptorImpl desc : provider.getDescriptorImpls()){
                if (desc.getId().equals(descriptorImplID)){
                    return desc;
                }
            }
        }

        throw new NoSuchElementException("Could not find a descriptor with id: " 
                                         + descriptorImplID);

    }


    public boolean existsDescriptor(String descriptorID) {

        for (Descriptor desc : getFullDescriptors()){
            if (desc.getId().equals(descriptorID)){
                return true;
            }
        }

        return false;

    }

    public boolean existsDescriptorImpl(String descriptorImplID) {

        for (DescriptorProvider provider : getFullProviders()){
            for (DescriptorImpl desc : provider.getDescriptorImpls()){
                if (desc.getId().equals(descriptorImplID)){
                    return true;
                }
            }
        }

        return false;

    }


    /**
     * Returns preferred impl for a descriptorID or null if none existing
     */
    public DescriptorImpl getPreferredImpl(String descriptorID){

        //Read preference and get order of providers

        IEclipsePreferences prefs = new DefaultScope().getNode(Activator.PLUGIN_ID);
        String ret=prefs.get(QSARConstants.QSAR_PROVIDERS_ORDER_PREFERENCE, null);
        if (ret==null || ret.equalsIgnoreCase( "error" )){
            //If empty, initialize prefs from scratch
            new QSARPreferenceInitializer().initializeDefaultPreferences();
            ret=prefs.get(QSARConstants.QSAR_PROVIDERS_ORDER_PREFERENCE, null);
            if (ret==null || ret.equalsIgnoreCase( "error" )){
                //If still empty, give up
                logger.equals( "Could not get default DescrProvider." );
                return null;
            }
        }

        //String of names
        String[] provArray=QsarPreferenceHelper.parseQsarPreferenceString(ret);

        for (String providerName : provArray){
            String providerID=QsarPreferenceHelper.getProviderID(providerName);
            if (providerID!=null){
                DescriptorProvider prov = getProviderByID(providerID);
                if (prov!=null){
                    for (DescriptorImpl impl : prov.getDescriptorImpls()){
                        if (impl.getDefinition().equals(descriptorID))
                            return impl;
                    }
                }
            } else{
                logger.error("Could not locate provider by name: " + providerName);
            }
        }
        //No impl found for this descrID
        return null;
    }


    /**
     * Get descriptor implementation be descriptorID and providerID or null if
     * none matching.
     */
    public DescriptorImpl getDescriptorImpl(String descriptorID, String providerID) {

        for (String descriptorImplID : getDescriptorImplsByProvider(providerID)){
            DescriptorImpl impl = getDescriptorImplByID(descriptorImplID);
            if (impl.getDefinition().equals(descriptorID))
                return impl;
        }

        return null;
    }




    /*
     * Below are necessary?
     * 
     * 
     */





    /*====================================================
     * Calculations below
     * ====================================================
     */



    /**
     * Calculate descriptors for N molecules with D descriptors with P params.
     * @param molecules List<IMolecule> containing the molecules
     * @param descriptorMap Map from descriptorImplID to List<DescriptorParameter>
     * @return
     */
    public Map<? extends IMolecule, List<IDescriptorResult>> calculate(
                                                                       List<? extends IMolecule> molecules, 
                                                                       List<DescriptorType> descriptorTypes, IProgressMonitor monitor)   throws OperationCanceledException{

        Map<IMolecule, List<DescriptorType>> molDescMap=new HashMap<IMolecule, List<DescriptorType>>();

        for (IMolecule mol : molecules){
            molDescMap.put( mol, descriptorTypes );
        }

        return calculate( molDescMap, monitor );

        //        Map<IMolecule, List<IDescriptorResult>> allResults=
        //            new HashMap<IMolecule, List<IDescriptorResult>>();
        //
        //        //The problem is to collect all descriptor ID's and group by provider
        //        //Loop over all providers
        //        for (DescriptorProvider provider : getFullProviders()){
        //
        //            //Store descriptors and params to calculate for this provider in list
        //            List<DescriptorType> descriptorTypesForProvider = new ArrayList<DescriptorType>();
        //
        //
        //
        //            //Check if this descriptor is here, add if so
        //            for (DescriptorType descType: descriptorTypes){
        //                String descImplId = descType.getProvider();
        //                DescriptorProvider prov = getProviderByID( descImplId );
        //                if (provider.getId().equals(prov.getId())){
        //                    descriptorTypesForProvider.add(descType);
        //                }
        //            }
        //
        //
        //            //If we have descs to calculate, do so
        //            if (descriptorTypesForProvider.size()>0){
        //                IDescriptorCalculator calculator=provider.getCalculator();
        //                Map<? extends IMolecule, List<IDescriptorResult>> results = 
        //                    calculator.calculateDescriptor(molecules, 
        //                                                   descriptorTypesForProvider, monitor);
        //
        //                //Add these results to the molecule
        //                for (IMolecule mol : results.keySet()){
        //                    if (allResults.get(mol)==null) allResults.put(mol, new ArrayList<IDescriptorResult>());
        //                    List<IDescriptorResult> reslist=allResults.get(mol);
        //
        //                    //Add the computed result to the reslist
        //                    reslist.addAll(results.get(mol));
        //                }
        //
        //            }
        //
        //        }
        //
        //        return allResults;
    }


    //	/**
    //	 * Convenience method to calculate descriptors without inputting parameters
    //	 */
    //	public Map<IMolecule, List<IDescriptorResult>> calculateNoParams(
    //			List<IMolecule> molecules, List<String> descriptorImplIDs) {
    //
    //		List<DescriptorType> insts=new ArrayList<DescriptorType>();
    //		
    //		for (String descImplID : descriptorImplIDs){
    //			DescriptorImpl impl=getDescriptorImplByID(descImplID);
    //			Descriptor desc=getDescriptorByID(impl.getDefinition());
    //			DescriptorType descType=createDescriptorType(desc, impl,null);
    //			
    //			DescriptorInstance inst=new DescriptorInstance(desc, impl,null);
    //			insts.add(inst);
    //		}
    //		
    //		return calculate(molecules, insts);
    //	}



    /**
     * Convenience method to calculate a descriptor for a single mol and 
     * a single descriptorID.
     */
    public IDescriptorResult calculate(IMolecule molecule, String descriptorID) {

        List<IMolecule> mollist=new ArrayList<IMolecule>();
        mollist.add(molecule);

        List<String> descIDs=new ArrayList<String>();
        descIDs.add(descriptorID);

        Map<? extends IMolecule, List<IDescriptorResult>> retMap = calculateNoParams(mollist, descIDs);
        if (retMap==null || retMap.size()<1){
            throw new NoSuchElementException("Calculation did not return a result");
        }
        List<IDescriptorResult> res=retMap.get(molecule);
        if (res==null || res.size()<=0)
            throw new NoSuchElementException("Calculation returned empty result");

        return res.get(0);

    }






    public List<IDescriptorResult> calculate(IMolecule molecule,
                                             List<DescriptorType> descriptorTypes) {

        List<IMolecule> mollist=new ArrayList<IMolecule>();
        mollist.add(molecule);

        Map<? extends IMolecule, List<IDescriptorResult>> ret = calculate(mollist, descriptorTypes, new NullProgressMonitor());

        return ret.get(molecule);
    }



    public Map<? extends IMolecule, List<IDescriptorResult>> calculateNoParams(
                                                                               List<? extends IMolecule> molecules, List<String> descriptorIDs) {
        List<DescriptorType> descTypes=new ArrayList<DescriptorType>();

        for (String descriptorID : descriptorIDs){
            //Look up descriptor by ID
            Descriptor desc=getDescriptorByID(descriptorID);
            if (desc==null){
                throw new IllegalArgumentException("Could not find descriptor: " + descriptorID);
            }

            //Loop up preferred implementation
            DescriptorImpl impl=getPreferredImpl(descriptorID);
            if (impl==null){
                throw new IllegalArgumentException("Could not find an implementation for descriptor: " + descriptorID);
            }


            DescriptorType descType=createDescriptorType(null, null, desc, impl,null);
            descTypes.add(descType);
        }

        return calculate(molecules, descTypes, new NullProgressMonitor());
    }



    /**
     * Add a descriptor with impl and optionally parameters to a QsarModel via 
     * an editingDomain
     * @param qsarModel
     * @param editingDomain
     * @param desc
     * @param impl
     * @param params
     * @return
     */
    public DescriptorType createDescriptorType(QsarType qsarModel, EditingDomain editingDomain, Descriptor desc,
                                               DescriptorImpl impl, List<DescriptorParameter> params) {

        //If qsarModel is null, create a new one
        //Used in tests
        qsarModel=QsarFactory.eINSTANCE.createQsarType();


        //Get and optionally create list of descriptors if null
        DescriptorlistType descriptorList = qsarModel.getDescriptorlist();
        if (descriptorList==null){
            descriptorList=QsarFactory.eINSTANCE.createDescriptorlistType();
            qsarModel.setDescriptorlist(descriptorList);
        }

        //If we have no editingDomain, create a basic one
        //Used in tests
        if (editingDomain==null){
            QsarAdapterFactory factory=new QsarAdapterFactory();
            editingDomain=new AdapterFactoryEditingDomain(factory, new BasicCommandStack());
        }


        //Collect all in a compound command, for ability 
        //to undo everything at the same time
        CompoundCommand cCmd = new CompoundCommand();
        Command cmd;

        DescriptorType modelDescriptor=QsarFactory.eINSTANCE.createDescriptorType();
        modelDescriptor.setId(generateUniqueDescriptorID(qsarModel));
        modelDescriptor.setOntologyid( desc.getId());
        cmd=AddCommand.create(editingDomain, descriptorList, QsarPackage.Literals.DESCRIPTORLIST_TYPE__DESCRIPTORS, modelDescriptor);
        cCmd.append(cmd);

        //Check if provider already added to qsarModel
        DescriptorproviderType dimpl=null;
        for (DescriptorproviderType pdimpl : qsarModel.getDescriptorproviders()){
            if (pdimpl.getId().equals(impl.getProvider().getId())){
                dimpl=QsarFactory.eINSTANCE.createDescriptorproviderType();
                dimpl.setId(pdimpl.getId());
            }
        }

        //If this is a new provider, add it to Qsar model
        if (dimpl==null){
            DescriptorProvider prov = impl.getProvider();

            String pid=prov.getId();
            String pname=prov.getName();
            String pvend=prov.getVendor();
            String pvers=prov.getVersion();
            String pns=prov.getNamesapce();

            //Create a provider (=descrProviderType) in qsar model root
            DescriptorproviderType newdimpl=QsarFactory.eINSTANCE.createDescriptorproviderType();
            newdimpl.setId(pid);
            newdimpl.setURL(pns);
            newdimpl.setVendor(pvend);
            newdimpl.setName(pname);
            newdimpl.setVersion(pvers);
            cmd=AddCommand.create(editingDomain, qsarModel, QsarPackage.Literals.QSAR_TYPE__DESCRIPTORPROVIDERS, newdimpl);
            cCmd.append(cmd);

            //Reference the created impl by ID
            dimpl=QsarFactory.eINSTANCE.createDescriptorproviderType();
            dimpl.setId(newdimpl.getId());

        }

        modelDescriptor.setProvider( dimpl.getId() );

        //        //Add found provider to descriptor element
        //        cmd=SetCommand.create(editingDomain, modelDescriptor, QsarPackage.Literals.DESCRIPTOR_TYPE__PROVIDER, dimpl);
        //        cCmd.append(cmd);

        //Parameters
        if (impl.getParameters()!=null){
            for (DescriptorParameter param : impl.getParameters()){

                ParameterType modelParam=QsarFactory.eINSTANCE.createParameterType();
                modelParam.setKey(param.getKey());

                //Set value from imple (=default)
                modelParam.setValue(param.getValue());

                //Check if provided parameters have values
                //If so, use it
                if (params!=null){
                    for (DescriptorParameter inparam : params){
                        if (inparam.getKey().equals(param.getKey())){
                            //We have input params, use value
                            modelParam.setValue(inparam.getValue());
                        }					}
                }
                cmd=AddCommand.create(editingDomain, modelDescriptor, QsarPackage.Literals.DESCRIPTOR_TYPE__PARAMETER, modelParam);
                cCmd.append(cmd);

            }
        }
        //Execute the compound command
        editingDomain.getCommandStack().execute(cCmd);

        return modelDescriptor;
    }


    /**
     * Get a new unique descriptorID by combining String descriptor with the 
     * lowest available int
     * @param qsarModel
     * @return
     */
    private String generateUniqueDescriptorID( QsarType qsarModel ) {

        //Build arraylist of existing IDs
        List<String> existingIDs=new ArrayList<String>();
        for (DescriptorType desc : qsarModel.getDescriptorlist().getDescriptors()){
            existingIDs.add( desc.getId() );
        }

        int cnt=1;
        String prefix="descriptor";
        while(existingIDs.contains( prefix+cnt )){
            cnt++;
        }

        return prefix+cnt;
    }



    public Map<IMolecule, List<IDescriptorResult>> calculate(
                                                             Map<IMolecule, List<DescriptorType>> molDescMap,
                                                             IProgressMonitor monitor ) {


        Map<IMolecule, List<IDescriptorResult>> allResults=
            new HashMap<IMolecule, List<IDescriptorResult>>();

        //We need to perform one QSAR calculation per provider
        //So, collect them by provider from input Map
        Map<DescriptorProvider,Map<IMolecule, List<DescriptorType>>> moldescByProvider 
        = new HashMap<DescriptorProvider, Map<IMolecule,List<DescriptorType>>>();

        //For all mols
        for (IMolecule mol : molDescMap.keySet()){
            List<DescriptorType> moldescriptors = molDescMap.get( mol );

            //For all descr
            for (DescriptorType desc : moldescriptors){
                String providerID=desc.getProvider();
                DescriptorProvider provider = getProviderByID( providerID );
                if (!(moldescByProvider.containsKey( provider ))){
                    //If not exists, create it
                    moldescByProvider.put( provider, new HashMap<IMolecule, List<DescriptorType>>() );
                }
                Map<IMolecule, List<DescriptorType>> localMolDesc 
                = (Map<IMolecule, List<DescriptorType>>) moldescByProvider.get( provider );

                if (!(localMolDesc.containsKey( mol ))){
                    localMolDesc.put( mol, new ArrayList<DescriptorType>() );
                }

                List<DescriptorType> localTypes=localMolDesc.get( mol );
                if (!(localTypes.contains( desc ))){
                    localTypes.add( desc );
                }
            }
        }


        //Process one provider at a time
        for (DescriptorProvider provider : moldescByProvider.keySet()){

            IDescriptorCalculator calculator=provider.getCalculator();

            Map<IMolecule, List<DescriptorType>> moldesc = moldescByProvider.get( provider );

            Map<? extends IMolecule, List<IDescriptorResult>> results = 
                calculator.calculateDescriptor(moldesc, monitor);

            //Add these results to the molecule
            for (IMolecule mol : results.keySet()){
                if (allResults.get(mol)==null) allResults.put(mol, new ArrayList<IDescriptorResult>());
                List<IDescriptorResult> reslist=allResults.get(mol);

                //Add the computed result to the reslist
                reslist.addAll(results.get(mol));
            }

        }

        return allResults;

    }

    //===============================
    //QSAR model operatios below
    //===============================

    /**
     * Add resources to QSAR model.
     */
    public void addResourcesToQsarModel(QsarType qsarmodel, EditingDomain editingDomain, 
                                        List<IResource> resourcesToAdd, final IProgressMonitor monitor) throws IOException, BioclipseException, CoreException {

        ICDKManager cdk = net.bioclipse.cdk.business.Activator.getDefault().getJavaCDKManager();
        IInChIManager inchi = net.bioclipse.inchi.business.Activator
            .getDefault().getJavaInChIManager();

        StructurelistType structList = qsarmodel.getStructurelist();
        CompoundCommand ccmd=new CompoundCommand();

        //Intermediate storage to keep track of what we have added, 
        //in order to get unique structureIds
        List<String> storedStructureIDs=new ArrayList<String>();

        for (IResource resource : resourcesToAdd){

            if (resource instanceof IFile) {
                IFile file = (IFile) resource;

                //Check if this file is already in model
                for (ResourceType existingRes : structList.getResources()){
                    if (existingRes.getName().equals(file.getName())){
                        throw new UnsupportedOperationException("File: " + 
                                                                file.getName() + 
                        " already exists in QSAR analysis.");
                    }
                }

                //Load molecules into file
                List<ICDKMolecule> mollist = cdk.loadMolecules(file);
                if (mollist==null || mollist.size()<=0){
                    throw new BioclipseException("No molecules in file");
                }

                //Count no of 2D and 3D
                int no2d=0;
                int no3d=0;
                for (ICDKMolecule mol : mollist){
                    if (cdk.has2d( mol ))
                        no2d++;
                    if (cdk.has3d( mol ))
                        no3d++;
                }

                //Add resource to QSAR model
                ResourceType res=QsarFactory.eINSTANCE.createResourceType();
                res.setId(file.getName());
                res.setName(file.getName());
                res.setFile(file.getFullPath().toString());
                res.setNo2d( no2d );
                res.setNo3d( no3d );
                res.setNoMols( mollist.size() );
                Command cmd=AddCommand.create(editingDomain, structList, 
                                              QsarPackage.Literals.STRUCTURELIST_TYPE__RESOURCES, res);
                ccmd.append(cmd);

                //Add all structures in resource as well as children to resource
                int molindex=0;
                for (ICDKMolecule mol : mollist){

                    StructureType structure=QsarFactory.eINSTANCE.createStructureType();

                    if (mol.getName()!=null && mol.getName().length()>0){
                        if (existsStructureIDInModel(qsarmodel, mol.getName())){
                            //Use a generated structureID
                            structure.setId( getStructureName(resource,molindex) );
                        }else{
                            if (storedStructureIDs.contains( mol.getName() )){
                                //Use a generated structureID
                                structure.setId( getStructureName(resource,molindex) );
                            }else{
                                //IDs should not start with _
                                if (mol.getName().startsWith( "_" )){
                                    //Use a generated structureID
                                    structure.setId( getStructureName(resource,molindex) );
                                }else{
                                    //This id is free and can be used
                                    structure.setId( mol.getName() );
                                }
                            }
                        }
                    }else{
                        //Use a generated structureID
                        structure.setId( getStructureName(resource,molindex) );
                    }

                    storedStructureIDs.add( structure.getId() );

                    //If text-based (currently the only supported method in Bioclipse)
                    structure.setResourceindex( molindex );

                    //FIXME: set structure changed in preferences!
                    //                    structure.setChanged( true );

                    //Calculate and add inchi to structure
                    try {
                        String inchistr = mol.getInChI(
                            net.bioclipse.core.domain.IMolecule
                                .Property.USE_CACHED_OR_CALCULATED
                        );
                        structure.setInchi( inchistr );
                    } catch ( Exception e ) {
                        logger.error("Could not generate inchi for mol " + 
                                     molindex + " in file " + file.getName());
                    }

                    cmd=AddCommand.create(editingDomain, res, 
                                          QsarPackage.Literals.RESOURCE_TYPE__STRUCTURE, structure);
                    ccmd.append(cmd);

                    molindex++;
                }

            }
        }

        //Execute the CompoundCommand
        editingDomain.getCommandStack().execute(ccmd);    

    }


    /**
     * Check if this newid already exists in model
     * @param qsarmodel
     * @param newid
     * @return
     */
    private boolean existsStructureIDInModel( QsarType qsarmodel, String newid ) {

        for (ResourceType res : qsarmodel.getStructurelist().getResources()){
            for (StructureType structure : res.getStructure()){
                if (structure.getId().equals( newid ))
                    return true;
            }
        }
        return false;
    }



    /**
     * This method generates a name from a resource with an index
     * @param resource
     * @param molindex
     * @return
     */
    private String getStructureName( IResource resource, int molindex ) {

        String inputname=resource.getName();
        if (molindex<=0)
            return inputname;

        String name=inputname.substring( 0, inputname.length()-4 );
        String ext=inputname.substring( inputname.length()-4, inputname.length() );
        //        return name +"_"+ molindex + ext;
        return name + ext +"-"+ molindex;

    }




    /**
     * Go through and add transient properties to EMF model, which are not 
     * stored in file.
     */
    public void addCalculatedPropertiesToQsarModel( QsarType qsarModel ) {

        ICDKManager cdk = net.bioclipse.cdk.business.Activator.getDefault().getJavaCDKManager();

        //Do resources first
        for (ResourceType resource : qsarModel.getStructurelist().getResources()){
            try {
                List<ICDKMolecule> mols = cdk.loadMolecules( resource.getFile());

                //Count no of 2D and 3D
                int no2d=0;
                int no3d=0;
                for (ICDKMolecule mol : mols){
                    if (cdk.has2d( mol ))
                        no2d++;
                    if (cdk.has3d( mol ))
                        no3d++;
                }

                resource.setNo2d( no2d );
                resource.setNo3d( no3d );
                resource.setNoMols( mols.size() );
            } catch ( Exception e ) {
                logger.error("Error parsing file: " + resource.getFile());
            }
        }
    }



    public void removeResourcesFromModel( QsarType qsarModel,
                                          EditingDomain editingDomain, List<ResourceType> list ) {

        CompoundCommand ccmd=new CompoundCommand();


        for (ResourceType resource : list){

            //Remove this resource, will remove responses too
            Command cmd=RemoveCommand.create(editingDomain, 
                                             qsarModel.getStructurelist(), 
                                             QsarPackage.Literals.STRUCTURELIST_TYPE__RESOURCES, 
                                             resource);
            ccmd.append(cmd);

            //Also remove all responses, if any
            if (qsarModel.getResponselist()!=null && qsarModel.getResponselist().getResponse().size()>0){

                for (StructureType structure : resource.getStructure()){
                    for (ResponseType response : qsarModel.getResponselist().getResponse()){
                        if (response.getStructureID().equals( structure.getId() )){
                            //Remove this response
                            cmd=RemoveCommand.create(editingDomain, 
                                                     qsarModel.getResponselist(), QsarPackage.Literals.
                                                     RESPONSES_LIST_TYPE__RESPONSE, response);
                            ccmd.append(cmd);
                        }
                    }
                }
            }
            
            //Also remove all descriptorresults, if any
            if (qsarModel.getDescriptorresultlist()!=null && qsarModel.getDescriptorresultlist().getDescriptorresult().size()>0){
                for (DescriptorresultType descres : qsarModel.getDescriptorresultlist().getDescriptorresult()){

                    for (StructureType structure : resource.getStructure()){
                        if (descres.getStructureid().equals( structure.getId() )){
                            //Remove this descriptorresult
                            cmd=RemoveCommand.create(editingDomain, 
                                                     qsarModel.getDescriptorresultlist(), 
                                                     QsarPackage.Literals.
                                                     DESCRIPTORRESULTLISTS_TYPE__DESCRIPTORRESULT, descres);
                            ccmd.append(cmd);
                        }
                    }
                }
            }


        }

        //Execute all commands in a batch
        editingDomain.getCommandStack().execute(ccmd);

    }


    /**
     * Add a descriptor to the QSAR model
     */
    public void addDescriptorToModel( QsarType qsarModel, 
                                      EditingDomain editingDomain,
                                      Descriptor desc, 
                                      DescriptorImpl impl ) {

        //Collect all in a compound command, for ability 
        //to undo everything at the same time
        CompoundCommand cCmd = new CompoundCommand();
        Command cmd;

        DescriptorType modelDescriptor=QsarFactory.eINSTANCE.createDescriptorType();
        modelDescriptor.setId(generateUniqueDescriptorID( qsarModel ));
        modelDescriptor.setOntologyid( desc.getId());

        //Check if provider already added to qsarModel
        DescriptorproviderType dprov=null;
        for (DescriptorproviderType pdimpl : qsarModel.getDescriptorproviders()){
            if (pdimpl.getId().equals(impl.getProvider().getId())){
                dprov=QsarFactory.eINSTANCE.createDescriptorproviderType();
                dprov.setId(pdimpl.getId());
            }
        }

        //If this is a new provider, add it to Qsar model
        if (dprov==null){
            DescriptorProvider prov = impl.getProvider();

            String pid=prov.getId();
            String pname=prov.getName();
            String pvend=prov.getVendor();
            String pvers=prov.getVersion();
            String pns=prov.getNamesapce();

            //Create a provider (=descrImplType) in qsar model root
            DescriptorproviderType newdprov=QsarFactory.eINSTANCE.createDescriptorproviderType();
            newdprov.setId(pid);
            newdprov.setURL( pns);
            newdprov.setVendor(pvend);
            newdprov.setName(pname);
            newdprov.setVersion(pvers);
            cmd=AddCommand.create(editingDomain, qsarModel, QsarPackage.Literals.QSAR_TYPE__DESCRIPTORPROVIDERS, newdprov);
            cCmd.append(cmd);

            //Reference the created impl by ID
            dprov=QsarFactory.eINSTANCE.createDescriptorproviderType();
            dprov.setId(newdprov.getId());

        }

        modelDescriptor.setProvider( dprov.getId() );

        //Add found impl to descriptor element
        //      cmd=SetCommand.create(editingDomain, modelDescriptor, QsarPackage.Literals.DESCRIPTOR_TYPE__PROVIDER, dprov.getId());
        //      cCmd.append(cmd);

        //Parameters
        if (impl.getParameters()!=null){
            for (DescriptorParameter param : impl.getParameters()){

                ParameterType modelParam=QsarFactory.eINSTANCE.createParameterType();
                modelParam.setKey(param.getKey());
                modelParam.setValue(param.getValue());
                cmd=AddCommand.create(editingDomain, modelDescriptor, QsarPackage.Literals.DESCRIPTOR_TYPE__PARAMETER, modelParam);
                cCmd.append(cmd);

            }
        }

        //Add the descriptor to descriptorList last, for notification issues
        cmd=AddCommand.create(editingDomain, qsarModel.getDescriptorlist(), QsarPackage.Literals.DESCRIPTORLIST_TYPE__DESCRIPTORS, modelDescriptor);
        cCmd.append(cmd);

        //Execute the compound command
        editingDomain.getCommandStack().execute(cCmd);        
    }



    public void removeDescriptorsFromModel( QsarType qsarModel,
                                            EditingDomain editingDomain,
                                            List<DescriptorType> list ) {

        CompoundCommand ccmd=new CompoundCommand();

        //Collect commands from selection
        for (DescriptorType descType : list){

            Command cmd=RemoveCommand.create(editingDomain, qsarModel.getDescriptorlist(), QsarPackage.Literals.DESCRIPTORLIST_TYPE__DESCRIPTORS, descType);
            ccmd.append(cmd);
            logger.debug("Removing descriptor: " + descType.getId());

            //Also delete any descriptorresults for this descriptor
            for (DescriptorresultType dres : qsarModel.getDescriptorresultlist().getDescriptorresult()){
                if (dres.getDescriptorid().equals( descType.getId() )){
                    cmd=RemoveCommand.create(editingDomain, qsarModel.getDescriptorresultlist(), QsarPackage.Literals.DESCRIPTORRESULTLISTS_TYPE__DESCRIPTORRESULT, dres);
                    ccmd.append(cmd);
                    logger.debug("   Removing corresponding descriptorresult: " + dres);
                }
            }

            //Check for unused descriptorproviders and remove them too
            for (DescriptorproviderType prov : qsarModel.getDescriptorproviders()){
                boolean remove=true;
                for (DescriptorType desc : qsarModel.getDescriptorlist().getDescriptors()){
                    if (desc.getProvider().equals( prov.getId() )){
                        //Nope, still used
                        remove=false;
                    }
                }
                if (remove){
                    cmd=RemoveCommand.create(editingDomain, qsarModel.getDescriptorproviders(), QsarPackage.Literals.QSAR_TYPE__DESCRIPTORPROVIDERS, prov);
                    ccmd.append(cmd);
                    logger.debug("  No uses of qsar provider " + prov.getId() +" so removed.");
                }
            }

        }

        //Execute the commands as one 
        editingDomain.getCommandStack().execute(ccmd);

    }

    /**
     * Add a list of response values to the QSAR model
     */
    public void addResponseUnitToModel( QsarType qsarModel, 
                                      EditingDomain editingDomain,
                                      List<ResponseUnit> list) {

        //Collect all in a compound command, for ability 
        //to undo everything at the same time
        CompoundCommand cCmd = new CompoundCommand();
        Command cmd;
        for (ResponseUnit storedunit : list){
            
            ResponseunitType unit1=QsarFactory.eINSTANCE.createResponseunitType();
            unit1.setId( storedunit.getId() );
            unit1.setName( storedunit.getName() );
            unit1.setShortname( storedunit.getShortname() );
            unit1.setDescription( storedunit.getDescription());
            unit1.setURL( storedunit.getUrl() );
            
            cmd=AddCommand.create(editingDomain, qsarModel, 
                                  QsarPackage.Literals.QSAR_TYPE__RESPONSEUNIT, unit1);
            logger.debug("Adding response value: " + unit1.getId());
            cCmd.append(cmd);
        }

        //Execute the compound command
        editingDomain.getCommandStack().execute(cCmd);        
    }

    
    public void removeResponseUnitsFromModel( QsarType qsarModel,
                                            EditingDomain editingDomain,
                                            List<ResponseUnit> list ) {

        CompoundCommand ccmd=new CompoundCommand();

        //Collect commands from selection
        for (ResponseUnit unit : list){
            
            //Look up in model
            for ( ResponseunitType rtype : qsarModel.getResponseunit()){
                
                if (rtype.getId().equals( unit.getId() )){
                    Command cmd=RemoveCommand.create(editingDomain, qsarModel, 
                           QsarPackage.Literals.QSAR_TYPE__RESPONSEUNIT, rtype);
                    ccmd.append(cmd);
                    logger.debug("Removing response value: " + unit.getId());
                }
            }
        }

        //Execute the commands as one 
        editingDomain.getCommandStack().execute(ccmd);

    }

    
}
