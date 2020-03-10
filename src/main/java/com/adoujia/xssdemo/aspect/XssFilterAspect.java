package com.adoujia.xssdemo.aspect;

import com.adoujia.xssdemo.annoations.XssRule;
import com.adoujia.xssdemo.service.XssService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @author fangcheng
 * @since 2020-03-01 13:43
 */
@Aspect
@Component
@Slf4j
public class XssFilterAspect {

    @Autowired
    XssService xssService;


    /**
     * 切入点为controller包中的所有方法
     *
     * @param joinPoint 切点参数
     */
    @Before("execution(* com.adoujia.xssdemo.controller..*.*(..))")
    public void xssFilter(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        if (!method.isAnnotationPresent(PostMapping.class)) {
            return;
        }
        //获取方法的所有注解
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (!parameter.isAnnotationPresent(Valid.class)) {
                continue;
            }
            doParamFilter(parameter, args[i]);
        }

    }

    /**
     * 对类型中的所有String参数中做filter
     *
     * @param parameter body 类型数据
     * @param object    传入的数据
     */
    private void doParamFilter(Parameter parameter, Object object) {
        Class<?> type = parameter.getType();
        Field[] fields = type.getDeclaredFields();
        Method[] methods = type.getMethods();
        for (Field field : fields) {
            //只过滤String类型
            if (!Objects.equals(field.getType(), String.class)) {
                continue;
            }
            Method getMethod = findMethod(methods, field.getName(), true);
            Method setMethod = findMethod(methods, field.getName(), false);
            if (Objects.isNull(getMethod) || Objects.isNull(setMethod)) {
                continue;
            }
            try {
                String input = (String) getMethod.invoke(object);
                if (Objects.isNull(input)) {
                    continue;
                }
                boolean allowHtml = field.isAnnotationPresent(XssRule.class);
                String result = xssService.doXssClean(input, allowHtml);
                setMethod.invoke(object, result);
            } catch (Exception e) {
                log.error("wtf", e);
            }
        }
    }

    /**
     * 获得参数的get或set方法
     *
     * @param methods   所有方法
     * @param filedName 字段名
     * @param isGet     是否get方法
     * @return set/get 函数
     */
    private Method findMethod(Method[] methods, String filedName, boolean isGet) {
        String prefix = isGet ? "get" : "set";
        String methodName = prefix + filedName.toLowerCase();
        for (Method method : methods) {
            boolean matched = Objects.equals(
                    methodName, method.getName().toLowerCase())
                    && (method.getReturnType().equals(String.class) == isGet);
            if (matched) {
                return method;
            }
        }
        return null;
    }

}
