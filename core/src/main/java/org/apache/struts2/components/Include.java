/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts2.components;

import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.ValueStack;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.RequestUtils;
import org.apache.struts2.StrutsConstants;
import org.apache.struts2.StrutsException;
import org.apache.struts2.util.FastByteArrayOutputStream;
import org.apache.struts2.views.annotations.StrutsTag;
import org.apache.struts2.views.annotations.StrutsTagAttribute;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * <!-- START SNIPPET: javadoc -->
 * <p>Include a servlet's output (result of servlet or a JSP page).</p>
 * <p>Note: Any additional params supplied to the included page are <b>not</b>
 * accessible within the rendered page through the &lt;s:property...&gt; tag
 * since no valuestack will be created. You can, however, access them in a
 * servlet via the HttpServletRequest object or from a JSP page via
 * a scriptlet.</p>
 * <!-- END SNIPPET: javadoc -->
 *
 *
 * <!-- START SNIPPET: params -->
 * <ul>
 *      <li>value* (String) - jsp page to be included</li>
 * </ul>
 * <!-- END SNIPPET: params -->
 *
 *
 * <p><b>Examples</b></p>
 * <pre>
 * <!-- START SNIPPET: example -->
 * &lt;-- One: --&gt;
 * &lt;s:include value="myJsp.jsp" /&gt;
 *
 * &lt;-- Two: --&gt;
 * &lt;s:include value="myJsp.jsp"&gt;
 *    &lt;s:param name="param1" value="value2" /&gt;
 *    &lt;s:param name="param2" value="value2" /&gt;
 * &lt;/s:include&gt;
 *
 * &lt;-- Three: --&gt;
 * &lt;s:include value="myJsp.jsp"&gt;
 *    &lt;s:param name="param1"&gt;value1&lt;/s:param&gt;
 *    &lt;s:param name="param2"&gt;value2&lt;/s:param&gt;
 * &lt;/s:include&gt;
 * <!-- END SNIPPET: example -->
 *
 * <!-- START SNIPPET: exampledescription -->
 * Example one - do an include myJsp.jsp page
 * Example two - do an include to myJsp.jsp page with parameters param1=value1 and param2=value2
 * Example three - do an include to myJsp.jsp page with parameters param1=value1 and param2=value2
 * <!-- END SNIPPET: exampledescription -->
 * </pre>
 *
 */
@StrutsTag(name="include", tldTagClass="org.apache.struts2.views.jsp.IncludeTag", description="Include a servlet's output " +
                "(result of servlet or a JSP page)")
public class Include extends Component {

    private static final Logger LOG = LogManager.getLogger(Include.class);

    private static final String SYSTEM_ENCODING = Charset.defaultCharset().displayName();

    protected String value;
    private final HttpServletRequest req;
    private final HttpServletResponse res;
    private String defaultEncoding;       // Made non-static (during WW-4971 fix)
    private boolean useResponseEncoding = true;  // Added with WW-4971 fix (allows switch between usage of response or default encoding)

    public Include(ValueStack stack, HttpServletRequest req, HttpServletResponse res) {
        super(stack);
        this.req = req;
        this.res = res;
    }

    @Inject(StrutsConstants.STRUTS_I18N_ENCODING)
    public void setDefaultEncoding(String encoding) {
        defaultEncoding = encoding;
    }

    @Inject(value = StrutsConstants.STRUTS_TAG_INCLUDETAG_USERESPONSEENCODING, required=false)
    public void setUseResponseEncoding(String useEncoding) {
        useResponseEncoding = Boolean.parseBoolean(useEncoding);
    }

    public boolean end(Writer writer, String body) {
        String page = findString(value, "value", "You must specify the URL to include. Example: /foo.jsp");
        StringBuilder urlBuf = new StringBuilder();
        String encodingForInclude;

        if (useResponseEncoding) {
            encodingForInclude = res.getCharacterEncoding();  // Use response (page) encoding
            if (encodingForInclude == null || encodingForInclude.isEmpty()) {
                encodingForInclude = defaultEncoding;  // Revert to defaultEncoding when response (page) encoding is invalid
            }
        }
        else {
            encodingForInclude = defaultEncoding;  // Use default encoding (when useResponseEncoding is false)
        }

        // Add URL
        urlBuf.append(page);

        // Add request parameters
        if (!attributes.isEmpty()) {
            urlBuf.append('?');

            String concat = "";

            // Set parameters
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                Object name = entry.getKey();
                List values = (List) entry.getValue();

                for (Object value : values) {
                    urlBuf.append(concat);
                    urlBuf.append(name);
                    urlBuf.append('=');

                    urlBuf.append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));

                    concat = "&";
                }
            }
        }

        String result = urlBuf.toString();

        // Include
        try {
            include(result, writer, req, res, encodingForInclude);
        } catch (ServletException | IOException e) {
            LOG.warn("Exception thrown during include of {}", result, e);
        }

        return super.end(writer, body);
    }

    @StrutsTagAttribute(description="The jsp/servlet output to include", required=true)
    public void setValue(String value) {
        this.value = value;
    }

    public static String getContextRelativePath(ServletRequest request, String relativePath) {
        String returnValue;

        if (relativePath.startsWith("/")) {
            returnValue = relativePath;
        } else if (!(request instanceof HttpServletRequest hrequest)) {
            returnValue = relativePath;
        } else {
            String uri = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);

            if (uri == null) {
                uri = RequestUtils.getServletPath(hrequest);
            }

            returnValue = uri.substring(0, uri.lastIndexOf('/')) + '/' + relativePath;
        }

        // .. is illegal in an absolute path according to the Servlet Spec and will cause
        // known problems on Orion application servers.
        if (returnValue.contains("..")) {
            Stack<String> stack = new Stack<>();
            StringTokenizer pathParts = new StringTokenizer(returnValue.replace('\\', '/'), "/");

            while (pathParts.hasMoreTokens()) {
                String part = pathParts.nextToken();

                if (!part.equals(".")) {
                    if (part.equals("..")) {
                        stack.pop();
                    } else {
                        stack.push(part);
                    }
                }
            }

            StringBuilder flatPathBuffer = new StringBuilder();

            for (int i = 0; i < stack.size(); i++) {
                flatPathBuffer.append("/").append(stack.elementAt(i));
            }

            returnValue = flatPathBuffer.toString();
        }

        return returnValue;
    }

    public void addParameter(String key, Object value) {
        // Don't use the default implementation of addParameter,
        // instead, include tag requires that each parameter be a list of objects,
        // just like the HTTP servlet interfaces are (String[])
        if (value != null) {
            List currentValues = (List) attributes.get(key);

            if (currentValues == null) {
                currentValues = new ArrayList();
                attributes.put(key, currentValues);
            }

            currentValues.add(value);
        }
    }

    /**
     * Include a resource in a response.
     *
     * @param relativePath the relative path of the resource to include; resolves to {@link #getContextRelativePath(jakarta.servlet.ServletRequest,
     *                     String)}
     * @param writer       the Writer to write output to
     * @param request      the current request
     * @param response     the response to write to
     * @param encoding     the file encoding to use for including the resource; if <tt>null</tt>, it will default to the
     *                     platform encoding
     *
     * @throws ServletException in case of servlet processing errors
     * @throws IOException in case of IO errors
     */
    public static void include( String relativePath, Writer writer, ServletRequest request,
                                HttpServletResponse response, String encoding ) throws ServletException, IOException {
        String resourcePath = getContextRelativePath(request, relativePath);
        RequestDispatcher rd = request.getRequestDispatcher(resourcePath);

        if (rd == null) {
            throw new ServletException("Not a valid resource path:" + resourcePath);
        }

        PageResponse pageResponse = new PageResponse(response);

        // Include the resource
        rd.include(request, pageResponse);

        if (encoding != null) {
            // Use given encoding
            pageResponse.getContent().writeTo(writer, encoding);
        } else {
            // Use the platform specific encoding
            pageResponse.getContent().writeTo(writer, SYSTEM_ENCODING);
        }
    }

    /**
     * Implementation of ServletOutputStream that stores all data written
     * to it in a temporary buffer accessible from {@link #getBuffer()} .
     *
     * @author <a href="joe@truemesh.com">Joe Walnes</a>
     * @author <a href="mailto:scott@atlassian.com">Scott Farquhar</a>
     */
    static final class PageOutputStream extends ServletOutputStream {

        private final FastByteArrayOutputStream buffer;


        public PageOutputStream() {
            buffer = new FastByteArrayOutputStream();
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            try {
                writeListener.onWritePossible();
            } catch (IOException e) {
                throw new StrutsException(e);
            }
        }

        /**
         * Return all data that has been written to this OutputStream.
         */
        public FastByteArrayOutputStream getBuffer() throws IOException {
            flush();

            return buffer;
        }

        @Override
        public void close() throws IOException {
            buffer.close();
        }

        @Override
        public void flush() throws IOException {
            buffer.flush();
        }

        @Override
        public void write(byte[] b, int o, int l) throws IOException {
            buffer.write(b, o, l);
        }

        @Override
        public void write(int i) throws IOException {
            buffer.write(i);
        }

        @Override
        public void write(byte[] b) throws IOException {
            buffer.write(b);
        }
    }


    /**
     * <p>
     * Simple wrapper to HTTPServletResponse that will allow getWriter()
     * and getResponse() to be called as many times as needed without
     * causing conflicts.
     * </p>
     * <p>
     * The underlying outputStream is a wrapper around
     * {@link PageOutputStream} which will store
     * the written content to a buffer.
     * </p>
     * <p>
     * This buffer can later be retrieved by calling {@link #getContent}.
     * </p>
     *
     * @author <a href="mailto:joe@truemesh.com">Joe Walnes</a>
     * @author <a href="mailto:scott@atlassian.com">Scott Farquhar</a>
     */
    static final class PageResponse extends HttpServletResponseWrapper {

        private PrintWriter pagePrintWriter;
        private PageOutputStream pageOutputStream = null;


        /**
         * Create PageResponse wrapped around an existing HttpServletResponse.
         */
        public PageResponse(HttpServletResponse response) {
            super(response);
        }

        /**
         * Return the content buffered inside the {@link PageOutputStream}.
         *
         * @return
         * @throws IOException
         */
        public FastByteArrayOutputStream getContent() throws IOException {
            // If we are using a writer, we need to flush the
            // data to the underlying outputstream.
            // Most containers do this - but it seems Jetty 4.0.5 doesn't
            if (pagePrintWriter != null) {
                pagePrintWriter.flush();
            }

            return ((PageOutputStream) getOutputStream()).getBuffer();
        }

        /**
         * Return instance of {@link PageOutputStream}
         * allowing all data written to stream to be stored in temporary buffer.
         */
        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (pageOutputStream == null) {
                pageOutputStream = new PageOutputStream();
            }

            return pageOutputStream;
        }

        /**
         * Return PrintWriter wrapper around PageOutputStream.
         */
        @Override
        public PrintWriter getWriter() throws IOException {
            if (pagePrintWriter == null) {
                pagePrintWriter = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
            }

            return pagePrintWriter;
        }
    }
}
