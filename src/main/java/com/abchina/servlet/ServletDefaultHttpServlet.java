package com.abchina.servlet;

import cn.hutool.core.util.StrUtil;
import com.abchina.util.FileUtils;
import org.springframework.util.StreamUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 *  默认servlet
 */
public class ServletDefaultHttpServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        com.abchina.servlet.ServletContext servletContext = (com.abchina.servlet.ServletContext) req.getServletContext();

        File jarFile = servletContext.getFile();
        String contextPath = "/";
        if(jarFile != null){
            contextPath = StrUtil.subBefore(jarFile.getName(), ".", true) + "/";
        }
        String path = StrUtil.subAfter(req.getRequestURI(), contextPath, true);

        byte[] bytes = FileUtils.readFileFromJar(jarFile, path);
        resp.getOutputStream().write(bytes);
    }
}
