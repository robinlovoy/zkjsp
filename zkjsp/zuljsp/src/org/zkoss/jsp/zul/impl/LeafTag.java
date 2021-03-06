/* LeafTag.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Fri Jul 20 17:07:47     2007, Created by tomyeh
}}IS_NOTE

Copyright (C) 2007 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.jsp.zul.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.JspTag;

import org.zkoss.lang.CommonException;
import org.zkoss.lang.reflect.Fields;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.CreateEvent;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zk.ui.ext.DynamicPropertied;
import org.zkoss.zk.ui.metainfo.ComponentDefinition;
import org.zkoss.zk.ui.metainfo.EventHandler;
import org.zkoss.zk.ui.metainfo.ZScript;
import org.zkoss.zk.ui.metainfo.impl.AnnotationHelper;
import org.zkoss.zk.ui.sys.ComponentCtrl;
import org.zkoss.zk.ui.sys.ComponentsCtrl;

/**
 * The skeletal class used to implement the JSP tag for ZK components
 * that don't accept any child.
 *
 * <p>Remember to declare the following in the TLD file:
 * &lt;body-content&gt;empty&lt;/body-content&gt;
 *
 * @author tomyeh, Ian Tsai
 * 
 */
abstract public class LeafTag extends AbstractTag implements DynamicAttributes, ComponentTag {
	protected Component _comp;
	protected RootTag _roottag;
	protected ComponentTag _parenttag;
	protected Map _attrMap = new LinkedHashMap();
	protected Map _eventListenerMap = new LinkedHashMap();
	protected String _use, _forward;
	protected ComposerHandler _composeHandle ;
    
	/** Returns the page tag that this tag belongs to.
	 */
	public RootTag getRootTag() {
		return _roottag;
	}
	/** Returns the parent tag.
	 */
	public ComponentTag getParentTag() {
		return _parenttag;
	}

	/** Returns the component associated with this tag.
	 */
	public Component getComponent() {
		return _comp;
	}

	//Deriving class must override//
	/**
	 * The name of the component from the ZK Component Definition
	 */
	abstract protected String getComponentName();

	//SimpleTagSupport//
	/** Sets the parent tag.
	 * Deriving class rarely need to invoke this method.
	 */
	public void setParent(JspTag parent) {
		super.setParent(parent);
		if(parent instanceof RootTag)
			((RootTag)parent).addRootComponent(this);
		
		final AbstractTag pt =
		(AbstractTag)findAncestorWithClass(this, AbstractTag.class);
		if (pt instanceof RootTag) { //root component tag
			_roottag = (RootTag)pt;
		} else if (pt instanceof ComponentTag) {
			_parenttag = (ComponentTag)pt;
			_roottag = _parenttag.getRootTag();
		} else {
			throw new IllegalStateException("Must be nested inside the page tag: "+this);
		}
		
	}
	
	/** To process the leaf tag.
	 * The deriving class rarely need to override this method.
	 */
	public void doTag() throws JspException, IOException {
		//System.out.println(">>> LeafTag::doTag(): <"+this.getJspTagName()+">");
		if (!isEffective())return; //nothing to do
		
		_composeHandle = new ComposerHandler(_attrMap.remove("apply"));
		try {// use-class initial works...
			
			initComponent(); //creates and registers the component
			doComponentContent();// do Component Content
			afterComposeComponent();//finish compose the component
			
		} catch (Throwable e) {
			doCatch(e);
		} finally {
			doFinally ();
		}
		
		writeComponentMark(); //write a special mark denoting the component
		
	}
	
	/*package*/ void doCatch (Throwable throwable) throws JspException{
		if (!_composeHandle.doCatch(throwable))
			throw new JspException(throwable);
	}
	
	/*package*/ void doFinally () throws JspException{
		_composeHandle.doFinally();
	}
	
	/** 
	 * Creates and registers the component.
	 * Called by {@link #doTag}.
	 */
	/*package*/ void initComponent() throws Exception  {
		if(_roottag==null)
			throw new IllegalStateException("Must be nested inside the page tag: "+this);
		
		String tagName = getComponentName();
		Page page = this._roottag.getPage();
		ComponentDefinition compdef = page.getComponentDefinition(tagName, true);
		if(compdef==null)
			throw new JspException("can't find this Component's definition:"+tagName);
		
		
		Component parent = _parenttag==null ? null : _parenttag.getComponent();
		//2013.01.24 Ian Tsai: just use null as argument
		_composeHandle.doBeforeCompose(page, parent, null);
		
		_comp = (Component) compdef.resolveImplementationClass(page, getUse()).newInstance();

		if (_parenttag != null)_parenttag.addChildTag(this);
		else _roottag.addChildTag(this);
	
		_comp.getDefinition().applyProperties(_comp);
	
		// apply attributes to component...
		evaluateDynaAttributes(_comp, _attrMap);
		//2012.08.31 Ian Tsai: move to here 
		_composeHandle.doBeforeComposeChildren(_comp);
	}
	/**
	 * 
	 * @throws Exception
	 */
	/*package*/ void doComponentContent() throws Exception {
		//Should do Nothing...
	}
	
	/**
	 * 
	 * Evaluate all attributes.
	 * If(is annotations)
	 * @param target the target component
	 * @param attrs
	 * @throws ModificationException
	 * @throws NoSuchMethodException
	 */
	protected void evaluateDynaAttributes(Component target, Map attrs) 
	throws CommonException, NoSuchMethodException{
		for(Iterator itor = attrs.entrySet().iterator();itor.hasNext();){
			Map.Entry entry= (Entry) itor.next();
			String attnm = (String)entry.getKey();
			Object value = entry.getValue();
			evaluateDynaAttribute(target, attnm, value);
		}
	}
	/**
	 * Test if the attribute are annotation or component attribute.<br>
	 * If(is Component attribute)Invokes setter method to update all 
	 * assigned attribute.
	 * If(is annotation)
	 * @param target
	 * @param attnm
	 * @param value
	 * @throws ModificationException
	 * @throws NoSuchMethodException
	 */
	public static void evaluateDynaAttribute(Component target, String attnm, Object value)
	throws CommonException, NoSuchMethodException
	{
		if(value!=null && value instanceof String)
		{
			String attval = value.toString();
			// test if this attribute is an annotation...
			if (AnnotationHelper.isAnnotation(attval)) { //annotation
				AnnotationHelper helper = new AnnotationHelper();
				helper.addByCompoundValue(attval, null);
				helper.applyAnnotations(target, 
						"self".equals(attnm) ? null: attnm, true);
			}
			else if(target.getDefinition().isMacro())
				((DynamicPropertied)target).setDynamicProperty(attnm, value);
			else Fields.setByCompound(target, attnm, value, true);
		}
		else if(target.getDefinition().isMacro())
			((DynamicPropertied)target).setDynamicProperty(attnm, value);
		else Fields.setByCompound(target, attnm, value, true);
	}
	
	/**
	 *   Called when a tag declared to accept dynamic attributes is passed an 
	 *   attribute that is not declared in the Tag Library Descriptor.<br>
	 *   
	 * @param uri the namespace of the attribute, always null currently.
	 * @param localName the name of the attribute being set.
	 * @param value  the value of the attribute
	 */
	public void setDynamicAttribute(String uri, String localName, Object value) 
	throws JspException {
		if("if".equals(localName)||"unless".equals(localName))
			throw new JspException("if, unless, use is static method!!!");
		
		if(localName.startsWith("on")){
			if(value==null){
				throw new IllegalArgumentException(
						"can not register a EventListener with null zscript " +
						"expression: "+localName);
			}
			// ZK6 MVVM annotation
			if ((value instanceof String) && ((String)value).trim().startsWith("@")) {
				System.out.println(localName);
				System.out.println(value);
				_attrMap.put(localName, value);
			} else // common java code
				_eventListenerMap.put(localName, value);
		}
		else
			_attrMap.put(localName, value);
	}
	

	/** Writes a special mark to the output to denote the location
	 * of this component.
	 * Called by {@link #doTag}.
	 */
	/*package*/ void writeComponentMark() throws IOException {
		if(isInline())
		{
			Component[] comps = getComponents();
			for(int i=0;i<comps.length;i++)
				Utils.writeComponentMark(getJspContext().getOut(),comps[i]);
		}
		else Utils.writeComponentMark(getJspContext().getOut(), _comp);
	}

	
	/** after children creation do dynamic attributes setter work and registers event handler.
	 * Called by {@link #doTag}.
	 * @throws JspException 
	 */
	/*package*/ void afterComposeComponent() throws JspException{

		
		if (_comp == null)
			throw new JspTagException("newComponent() returns null");
		

		
		if (_comp instanceof AfterCompose)//safty check...
			((AfterCompose)_comp).afterCompose();
		
		_composeHandle.doAfterCompose(_comp);// Composer fire...
		
		//process the forward condition...
		ComponentsCtrl.applyForward(_comp, _forward);

		//add & register event handle ...		
		for(Iterator itor = _eventListenerMap.entrySet().iterator();itor.hasNext();) {
			Map.Entry entry = (Map.Entry)itor.next();
			final ZScript zscript = ZScript.parseContent((String)entry.getValue());
			((ComponentCtrl)_comp).addEventHandler(
					(String)entry.getKey(), new EventHandler(zscript));
		}
		
		//fire onCreate event...
		if (Events.isListened(_comp, Events.ON_CREATE, false))//send onCreate event...
			Events.postEvent(
				new CreateEvent(Events.ON_CREATE, _comp, Executions.getCurrent().getArg()));
		
	}
    /**
     * Returns the class name that is used to implement the component
     * associated with this tag.
     *
     * <p>Default: null
     *
     * @return the class name used to implement the component, or null
     * to use the default
     */
    public String getUse() {
        return _use;
    }
    /**
     * Sets the class name that is used to implement the component
     * associated with this tag.
     *
     * @param use the class name used to implement the component, or null
     * to use the default
     */
    public void setUse(String use) {
        this._use = use;
    }
	/** Returns the forward condition that controls how to forward
	 * an event, that is received by the component created
	 * by this info, to another component.
	 *
	 * <p>Default: null.
	 *
	 * <p>If not null, when the component created by this
	 * info receives the event specified in the forward condition,
	 * it will forward it to the target component, which is also
	 * specified in the forward condition.
	 *
	 * @see #setForward
	 */
	public String getForward() {
		return _forward;
	}
	/** Sets the forward condition that controls when to forward
	 * an event receiving by this component to another component.
	 *
	 * <p>The basic format:<br/>
	 * <code>onEvent1=id1/id2.onEvent2</code>
	 *
	 * <p>It means when onEvent1 is received, onEvent2 will be posted
	 * to the component with the specified path (id1/id2).
	 *
	 * <p>If onEvent1 is omitted, it is assumed to be onClick (and
	 * the equal sign need not to be specified.
	 * If the path is omitted, it is assumed to be the space owner
	 * {@link Component#getSpaceOwner}.
	 *
	 * <p>For example, "onOK" means "onClick=onOK".
	 *
	 * <p>You can specify several forward conditions by separating
	 * them with comma as follows:
	 *
	 * <p><code>onChanging=onChanging,onChange=onUpdate,onOK</code>
	 *
	 * @param forward the forward condition. There are several forms:
	 * "onEvent1", "target.onEvent1" and "onEvent1(target.onEvent2)",
	 * where target could be "id", "id1/id2" or "${elExpr}".
	 * The EL expression must return either a path or a reference to
	 * a component.
	 */
	public void setForward(String forward) {
		_forward = forward != null && forward.length() > 0 ? forward: null;
	}
	/**
	 * default Tag's Component is not an inline macro.
	 */
	public boolean isInline() {
		return false;
	}
	/**
	 *  a dummy method of {@link LeafTag#getComponent()}
	 */
	public Component[] getComponents() {
		return new Component[]{_comp};
	}
}
