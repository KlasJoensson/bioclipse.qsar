/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.sf.bibtexml.impl;

import java.util.Collection;

import net.sf.bibtexml.BibTeXMLEntryType;
import net.sf.bibtexml.BibtexmlPackage;
import net.sf.bibtexml.FileType;

import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>File Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link net.sf.bibtexml.impl.FileTypeImpl#getEntry <em>Entry</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class FileTypeImpl extends EObjectImpl implements FileType {
    /**
     * The cached value of the '{@link #getEntry() <em>Entry</em>}' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getEntry()
     * @generated
     * @ordered
     */
    protected EList<BibTeXMLEntryType> entry;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    protected FileTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    protected EClass eStaticClass() {
        return BibtexmlPackage.Literals.FILE_TYPE;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public EList<BibTeXMLEntryType> getEntry() {
        if (entry == null) {
            entry = new EObjectContainmentEList<BibTeXMLEntryType>(BibTeXMLEntryType.class, this, BibtexmlPackage.FILE_TYPE__ENTRY);
        }
        return entry;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case BibtexmlPackage.FILE_TYPE__ENTRY:
                return ((InternalEList<?>)getEntry()).basicRemove(otherEnd, msgs);
        }
        return super.eInverseRemove(otherEnd, featureID, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case BibtexmlPackage.FILE_TYPE__ENTRY:
                return getEntry();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @SuppressWarnings("unchecked")
    @Override
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case BibtexmlPackage.FILE_TYPE__ENTRY:
                getEntry().clear();
                getEntry().addAll((Collection<? extends BibTeXMLEntryType>)newValue);
                return;
        }
        super.eSet(featureID, newValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public void eUnset(int featureID) {
        switch (featureID) {
            case BibtexmlPackage.FILE_TYPE__ENTRY:
                getEntry().clear();
                return;
        }
        super.eUnset(featureID);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public boolean eIsSet(int featureID) {
        switch (featureID) {
            case BibtexmlPackage.FILE_TYPE__ENTRY:
                return entry != null && !entry.isEmpty();
        }
        return super.eIsSet(featureID);
    }

} //FileTypeImpl
