package wang.ismy.spring.mvc;

import org.apache.commons.lang3.StringUtils;
import wang.ismy.spring.mvc.annotation.Controller;
import wang.ismy.spring.mvc.annotation.RequestMapping;
import wang.ismy.spring.mvc.utils.ClassUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.重写init
 * 扫包注入所有controller
 * 映射url与方法
 * 2.处理请求
 *
 * @author MY
 * @date 2020/1/14 12:02
 */
public class DispatcherServlet extends HttpServlet {

    private Map<String, Object> beans = new ConcurrentHashMap<>();
    private Map<String, Object> urlBeanMap = new ConcurrentHashMap<>();
    private Map<String, Method> urlMethodMap = new ConcurrentHashMap<>();

    @Override
    public void init() throws ServletException {
        try {
            // 创建所有controller
            initBean();
            // 创建URL与controller的method映射
            createUrlMapping();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 获取请求地址
        String uri = req.getRequestURI();
        // 根据地址获取controller和method
        Object controller = urlBeanMap.get(uri);
        if (controller == null){
            resp.sendError(404);
            return;
        }

        // 调用
        Method method = urlMethodMap.get(uri);
        try{
            Object result = method.invoke(controller);
            // 调用视图解析器渲染结果
            viewResolve(result.toString(),req,resp);
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    private void viewResolve(String pageName,HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF//views/"+pageName+".jsp").forward(req,resp);
    }

    private void initBean() throws Exception {
        // 扫包获取包下所有类
        List<Class<?>> classes = ClassUtil.getClasses(this.getClass().getPackageName());
        // 找到存在注解的controller并创建
        newInstanceExistAnnotation(classes);
    }

    private void createUrlMapping() {
        Collection<Object> controllers = beans.values();
        for (Object controller : controllers) {
            RequestMapping classRequestMapping = controller.getClass().getAnnotation(RequestMapping.class);
            String url = "";
            if (classRequestMapping != null) {
                url = classRequestMapping.value();
            }
            // 获取controller所有有注解的method
            // 将方法与URL映射到表里
            for (Method method : findMethodExistAnnotation(controller.getClass())) {
                RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
                String mappingUrl = url;
                mappingUrl = url + methodRequestMapping.value();

                urlMethodMap.put(mappingUrl, method);
                urlBeanMap.put(mappingUrl, controller);
            }
        }
    }

    private List<Method> findMethodExistAnnotation(Class<?> klass) {
        Method[] methods = klass.getMethods();
        List<Method> ret = new ArrayList<>();
        for (Method method : methods) {
            if (method.getAnnotation(RequestMapping.class) != null) {
                ret.add(method);
            }
        }
        return ret;
    }

    private void newInstanceExistAnnotation(List<Class<?>> classes) throws Exception {
        for (Class<?> aClass : classes) {
            if (aClass.getAnnotation(Controller.class) != null) {
                Object controller = aClass.getConstructor().newInstance();
                beans.put(toLowerCaseFirst(aClass.getSimpleName()),controller);
            }
        }
    }

    private String toLowerCaseFirst(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
