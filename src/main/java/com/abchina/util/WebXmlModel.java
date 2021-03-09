package com.abchina.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "web-app")
public class WebXmlModel {
    @JacksonXmlProperty(localName = "servlet")
    @JacksonXmlElementWrapper(useWrapping = false)
    private ServletNode[] servletNodes;

    @JacksonXmlProperty(localName = "servlet-mapping")
    @JacksonXmlElementWrapper(useWrapping = false)
    private ServletMappingNode[] servletMappingNodes;

    @JacksonXmlProperty(localName = "display-name")
    private String displayName;


    @JacksonXmlProperty(localName = "filter")
    @JacksonXmlElementWrapper(useWrapping = false)
    private FilterNode[] filterNodes;

    @JacksonXmlProperty(localName = "filter-mapping")
    @JacksonXmlElementWrapper(useWrapping = false)
    private FilterMapping[] filterMappings;

    public FilterNode[] getFilterNodes() {
        return filterNodes;
    }

    public void setFilterNodes(FilterNode[] filterNodes) {
        this.filterNodes = filterNodes;
    }

    public FilterMapping[] getFilterMappings() {
        return filterMappings;
    }

    public void setFilterMappings(FilterMapping[] filterMappings) {
        this.filterMappings = filterMappings;
    }

    public ServletNode[] getServletNodes() {
        return servletNodes;
    }

    public ServletMappingNode[] getServletMappingNodes() {
        return servletMappingNodes;
    }

    public void setServletNodes(ServletNode[] servletNodes) {
        this.servletNodes = servletNodes;
    }

    public void setServletMappingNodes(ServletMappingNode[] servletMappingNodes) {
        this.servletMappingNodes = servletMappingNodes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServletNode {
        @JacksonXmlProperty(localName = "servlet-name")
        private String servletName;

        @JacksonXmlProperty(localName = "servlet-class")
        private String servletClass;

        @JacksonXmlProperty(localName = "init-param")
        @JacksonXmlElementWrapper(useWrapping = false)
        private ServletInitParam[] servletInitParams;

        public String getServletName() {
            return servletName;
        }

        public void setServletName(String servletName) {
            this.servletName = servletName;
        }

        public String getServletClass() {
            return servletClass;
        }

        public void setServletClass(String servletClass) {
            this.servletClass = servletClass;
        }

        public ServletInitParam[] getServletInitParams() {
            return servletInitParams;
        }

        public void setServletInitParams(ServletInitParam[] servletInitParams) {
            this.servletInitParams = servletInitParams;
        }
    }

    public static class ServletMappingNode {
        @JacksonXmlProperty(localName = "servlet-name")
        private String servletName;

        @JacksonXmlProperty(localName = "url-pattern")
        private String urlPattern;

        public String getServletName() {
            return servletName;
        }

        public void setServletName(String servletName) {
            this.servletName = servletName;
        }

        public String getUrlPattern() {
            return urlPattern;
        }

        public void setUrlPattern(String urlPattern) {
            this.urlPattern = urlPattern;
        }
    }

    public static class ServletInitParam{
        @JacksonXmlProperty(localName = "param-name")
        private String paramName;

        @JacksonXmlProperty(localName = "param-value")
        private String paramValue;

        public String getParamName() {
            return paramName;
        }

        public void setParamName(String paramName) {
            this.paramName = paramName;
        }

        public String getParamValue() {
            return paramValue;
        }

        public void setParamValue(String paramValue) {
            this.paramValue = paramValue;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilterNode{
        @JacksonXmlProperty(localName = "filter-name")
        private String filterName;
        @JacksonXmlProperty(localName = "filter-class")
        private String filterClass;

        public String getFilterName() {
            return filterName;
        }

        public void setFilterName(String filterName) {
            this.filterName = filterName;
        }

        public String getFilterClass() {
            return filterClass;
        }

        public void setFilterClass(String filterClass) {
            this.filterClass = filterClass;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilterMapping{
        @JacksonXmlProperty(localName = "filter-name")
        private String filterName;
        @JacksonXmlProperty(localName = "url-pattern")
        private String urlPattern;

        public String getFilterName() {
            return filterName;
        }

        public void setFilterName(String filterName) {
            this.filterName = filterName;
        }

        public String getUrlPattern() {
            return urlPattern;
        }

        public void setUrlPattern(String urlPattern) {
            this.urlPattern = urlPattern;
        }
    }

}
