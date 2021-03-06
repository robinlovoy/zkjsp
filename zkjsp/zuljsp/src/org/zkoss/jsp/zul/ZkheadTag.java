/* ZkheadTag.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
	2008/11/25  19:22:53 , auto generated by Flyworld
}}IS_NOTE

Copyright (C) 2008 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
 */
package org.zkoss.jsp.zul;

import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.zkoss.jsp.zul.impl.Jsps;
import org.zkoss.jsp.zul.impl.RootTag;

import org.zkoss.util.logging.Log;
import org.zkoss.zk.fn.JspFns;

/**
 * The JSP tag to represent zkhead.
 * 
 * @author Flyworld
 * @since 1.3.0
 */
public class ZkheadTag extends SimpleTagSupport {
	private static final Log log = Log.lookup(ZkheadTag.class);

	/***
	 * 
	 * This tag will print zk html head.
	 */
	public void doTag() throws JspException, IOException {
		final JspContext jspctx = getJspContext();
		final PageContext pgctx = Jsps.getPageContext(jspctx);
		final ServletContext svlctx = pgctx.getServletContext();
		final HttpServletRequest request = (HttpServletRequest) pgctx
				.getRequest();
		final HttpServletResponse response = (HttpServletResponse) pgctx
				.getResponse();
		JspWriter out = getJspContext().getOut();

		try {
			out.write(JspFns.outZkHtmlTags(svlctx, request, response, null));
		} catch (NoClassDefFoundError e) {
			log.warning("ZkheadTag doesn't support ZK Version:" + org.zkoss.zk.Version.UID);
		}
	}


}