<%@ page import="com.liferay.portal.kernel.util.WebKeys" %>
<%@ page import="com.liferay.portal.theme.ThemeDisplay" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib prefix="aui" uri="http://alloy.liferay.com/tld/aui" %>

<portlet:defineObjects />

<%
//    List<BaseEntity> baseEntityList = (List<BaseEntity>)renderRequest.getAttribute("entityList");
%>

<portlet:resourceURL var="getDataURL">

    <%--<portlet:param name="metaId" value="testClass" />--%>

</portlet:resourceURL>

<link rel="stylesheet" media="all" href="/static-usci/ext/resources/css/ext-all.css" />
<link rel="stylesheet" media="all" href="<%=request.getContextPath()%>/css/main.css" />

<script>
    var dataUrl = '<%=getDataURL%>';
    var contextPathUrl = '<%=request.getContextPath()%>';
</script>

<script src="/static-usci/ext/ext-all.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/create_mc_form.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/create_mc_attr_form.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/main.js" type="text/javascript"></script>

<script src="<%=request.getContextPath()%>/js/lang/default.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/lang/<%= ((ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY)).getLocale() %>.js" type="text/javascript"></script>


<div id="meta-editor-content">

</div>