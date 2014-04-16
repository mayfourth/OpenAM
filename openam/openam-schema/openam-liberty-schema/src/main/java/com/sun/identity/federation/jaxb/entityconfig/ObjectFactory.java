//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-b27-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.06.11 at 10:33:54 AM PDT 
//


package com.sun.identity.federation.jaxb.entityconfig;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.sun.identity.federation.jaxb.entityconfig package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
public class ObjectFactory
    extends com.sun.identity.federation.jaxb.entityconfig.impl.runtime.DefaultJAXBContextImpl
{

    private static java.util.HashMap defaultImplementations = new java.util.HashMap(16, 0.75F);
    private static java.util.HashMap rootTagMap = new java.util.HashMap();
    public final static com.sun.identity.federation.jaxb.entityconfig.impl.runtime.GrammarInfo grammarInfo = new com.sun.identity.federation.jaxb.entityconfig.impl.runtime.GrammarInfoImpl(rootTagMap, defaultImplementations, (com.sun.identity.federation.jaxb.entityconfig.ObjectFactory.class));
    public final static java.lang.Class version = (com.sun.identity.federation.jaxb.entityconfig.impl.JAXBVersion.class);

    static {
        defaultImplementations.put((com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement.class), "com.sun.identity.federation.jaxb.entityconfig.impl.SPDescriptorConfigElementImpl");
        defaultImplementations.put((com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement.class), "com.sun.identity.federation.jaxb.entityconfig.impl.IDPDescriptorConfigElementImpl");
        defaultImplementations.put((com.sun.identity.federation.jaxb.entityconfig.ValueElement.class), "com.sun.identity.federation.jaxb.entityconfig.impl.ValueElementImpl");
        defaultImplementations.put((com.sun.identity.federation.jaxb.entityconfig.EntityConfigType.class), "com.sun.identity.federation.jaxb.entityconfig.impl.EntityConfigTypeImpl");
        defaultImplementations.put((com.sun.identity.federation.jaxb.entityconfig.AttributeType.class), "com.sun.identity.federation.jaxb.entityconfig.impl.AttributeTypeImpl");
        defaultImplementations.put((com.sun.identity.federation.jaxb.entityconfig.AffiliationDescriptorConfigElement.class), "com.sun.identity.federation.jaxb.entityconfig.impl.AffiliationDescriptorConfigElementImpl");
        defaultImplementations.put((com.sun.identity.federation.jaxb.entityconfig.BaseConfigType.class), "com.sun.identity.federation.jaxb.entityconfig.impl.BaseConfigTypeImpl");
        defaultImplementations.put((com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement.class), "com.sun.identity.federation.jaxb.entityconfig.impl.EntityConfigElementImpl");
        defaultImplementations.put((com.sun.identity.federation.jaxb.entityconfig.AttributeElement.class), "com.sun.identity.federation.jaxb.entityconfig.impl.AttributeElementImpl");
        rootTagMap.put(new javax.xml.namespace.QName("urn:sun:fm:ID-FF:entityconfig", "Value"), (com.sun.identity.federation.jaxb.entityconfig.ValueElement.class));
        rootTagMap.put(new javax.xml.namespace.QName("urn:sun:fm:ID-FF:entityconfig", "AffiliationDescriptorConfig"), (com.sun.identity.federation.jaxb.entityconfig.AffiliationDescriptorConfigElement.class));
        rootTagMap.put(new javax.xml.namespace.QName("urn:sun:fm:ID-FF:entityconfig", "Attribute"), (com.sun.identity.federation.jaxb.entityconfig.AttributeElement.class));
        rootTagMap.put(new javax.xml.namespace.QName("urn:sun:fm:ID-FF:entityconfig", "IDPDescriptorConfig"), (com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement.class));
        rootTagMap.put(new javax.xml.namespace.QName("urn:sun:fm:ID-FF:entityconfig", "EntityConfig"), (com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement.class));
        rootTagMap.put(new javax.xml.namespace.QName("urn:sun:fm:ID-FF:entityconfig", "SPDescriptorConfig"), (com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement.class));
    }

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.sun.identity.federation.jaxb.entityconfig
     * 
     */
    public ObjectFactory() {
        super(grammarInfo);
    }

    /**
     * Create an instance of the specified Java content interface.
     * 
     * @param javaContentInterface
     *     the Class object of the javacontent interface to instantiate
     * @return
     *     a new instance
     * @throws JAXBException
     *     if an error occurs
     */
    public java.lang.Object newInstance(java.lang.Class javaContentInterface)
        throws javax.xml.bind.JAXBException
    {
        return super.newInstance(javaContentInterface);
    }

    /**
     * Get the specified property. This method can only be
     * used to get provider specific properties.
     * Attempting to get an undefined property will result
     * in a PropertyException being thrown.
     * 
     * @param name
     *     the name of the property to retrieve
     * @return
     *     the value of the requested property
     * @throws PropertyException
     *     when there is an error retrieving the given property or value
     */
    public java.lang.Object getProperty(java.lang.String name)
        throws javax.xml.bind.PropertyException
    {
        return super.getProperty(name);
    }

    /**
     * Set the specified property. This method can only be
     * used to set provider specific properties.
     * Attempting to set an undefined property will result
     * in a PropertyException being thrown.
     * 
     * @param name
     *     the name of the property to retrieve
     * @param value
     *     the value of the property to be set
     * @throws PropertyException
     *     when there is an error processing the given property or value
     */
    public void setProperty(java.lang.String name, java.lang.Object value)
        throws javax.xml.bind.PropertyException
    {
        super.setProperty(name, value);
    }

    /**
     * Create an instance of SPDescriptorConfigElement
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement createSPDescriptorConfigElement()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.federation.jaxb.entityconfig.impl.SPDescriptorConfigElementImpl();
    }

    /**
     * Create an instance of IDPDescriptorConfigElement
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement createIDPDescriptorConfigElement()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.federation.jaxb.entityconfig.impl.IDPDescriptorConfigElementImpl();
    }

    /**
     * Create an instance of ValueElement
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.federation.jaxb.entityconfig.ValueElement createValueElement()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.federation.jaxb.entityconfig.impl.ValueElementImpl();
    }

    /**
     * Create an instance of ValueElement
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.federation.jaxb.entityconfig.ValueElement createValueElement(java.lang.String value)
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.federation.jaxb.entityconfig.impl.ValueElementImpl(value);
    }

    /**
     * Create an instance of EntityConfigType
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.federation.jaxb.entityconfig.EntityConfigType createEntityConfigType()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.federation.jaxb.entityconfig.impl.EntityConfigTypeImpl();
    }

    /**
     * Create an instance of AttributeType
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.federation.jaxb.entityconfig.AttributeType createAttributeType()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.federation.jaxb.entityconfig.impl.AttributeTypeImpl();
    }

    /**
     * Create an instance of AffiliationDescriptorConfigElement
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.federation.jaxb.entityconfig.AffiliationDescriptorConfigElement createAffiliationDescriptorConfigElement()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.federation.jaxb.entityconfig.impl.AffiliationDescriptorConfigElementImpl();
    }

    /**
     * Create an instance of BaseConfigType
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.federation.jaxb.entityconfig.BaseConfigType createBaseConfigType()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.federation.jaxb.entityconfig.impl.BaseConfigTypeImpl();
    }

    /**
     * Create an instance of EntityConfigElement
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement createEntityConfigElement()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.federation.jaxb.entityconfig.impl.EntityConfigElementImpl();
    }

    /**
     * Create an instance of AttributeElement
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.federation.jaxb.entityconfig.AttributeElement createAttributeElement()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.federation.jaxb.entityconfig.impl.AttributeElementImpl();
    }

}
