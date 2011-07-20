<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://www.zkoss.org/jsp/zul" prefix="z" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>include a jsp ...</title>
</head>
<%
request.setAttribute("bb", new Boolean(true));
%>
<body>
<h1>Test attribute...</h1>
	<z:page id="testPage">
		
		<z:window id="testWin" use="org.zkoss.jspdemo.MyWindow">
			<z:custom-attributes scope="component" test1="this is test one" test2="show test 2" ewrt="wqer"/>
			
			<z:button label="value">
				<z:attribute name="onClick">
					label1.value = testWin.getAttribute("test1");
					label2.value = pageScope.get("test2");
				</z:attribute>
			</z:button>	
			<z:label id="label1" value="[${testWin.title}]"/>
			<z:label id="label2" value="[${testWin.myValue}]"/>
		</z:window>
	</z:page>
</body>
</html>