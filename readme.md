## AOP+Jsoup 实现灵活xss防御

- 通过切面切入所有的接口方法
- 对其中所有加了@Valid（换为@RequestBody覆盖范围更广）的如参进行xss过滤
- 反射获取参数中的所有字段，找出其中的String类型字段
- 通过反射获取字段的get/set方法，用于获取初始输入及设置过滤后结果
- 对String类型字段执行过滤，通过自定义注解及对应白名单实现自定义过滤策略

### 切面及切面方法
定义一个XssFilter切面， 及一个切入所有的接口方法xssFilter方法。对所有在Post方法提交， 且带有@Valid注解的入参进行xss过滤。
 ```
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
 ```
### xss过滤实现
通过反射获取参数中的所有字段，遍历字段，对其中的String类型字段进行xss过滤。
这里加了一个自定义注解@XssRule, 同时有一个相对应的白名单，用于实现自定义注解。
- xss过滤实现逻辑
```
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
```
- 反射获取set/get方法
```
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
```
- XssRule
```
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XssRule {
}
```
- 自定义白名单，在默认白名单基础上允许style标签及其type属性
```
    private static Whitelist HTML_WHITELIST;

    /**
     * HTML 过滤策略
     */
    static {
        HTML_WHITELIST = Whitelist.relaxed();
        HTML_WHITELIST.addAttributes("style", "type");
    }
```
- 过滤参数方法， 如果需要设置多于两种过滤策略，修改第二个参数， 同时if/else改为switch即可
```
    /**
     * 执行不同的html过滤策略
     * @param input 过滤前字符串
     * @param allowHtml 是否允许传入html
     * @return
     */
    public String doXssClean(String input, boolean allowHtml) {
        if (allowHtml) {
            return Jsoup.clean(input, HTML_WHITELIST);
        } else {
            return Jsoup.clean(input, Whitelist.relaxed());
        }
    }
```
### 测试
- 定义测试类XssDTO，为其中一个字段加上自定义注解XssRule
```
@Data
public class XssTestDTO {
    String normal;
    @XssRule
    String html;
}
```
- 新增一个post接口方法， 如参为XssTestDTO， 直接返回XssTestDTO
```
@RestController
@Slf4j
public class XssTestController {

    @PostMapping("/xss/test")
    public XssTestDTO xssFilterTest(
            @RequestBody @Valid XssTestDTO xssTestDTO
    ) {
        return xssTestDTO;
    }
}
```
- 测试方法，对两个String字段设置相同的， 通过返回值查看过滤结果
```
    @Autowired
    MockMvc mockMvc;

    @Test
    public void xssFilterTest() throws Exception {
        String html = "<style type=\"text/css\"></style><p>text</p>";
        XssTestDTO xssTestDTO = new XssTestDTO();
        xssTestDTO.setHtml(html);
        xssTestDTO.setNormal(html);
        String content =
                mockMvc
                        .perform(
                                post("/xss/test")
                                        .content(JSON.toJSONString(xssTestDTO))
                                        .contentType(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();

        XssTestDTO result = JSON.parseObject(content, XssTestDTO.class);
        Document htmlDoc = Jsoup.parse(result.getHtml());
        Elements htmlElements = htmlDoc.select("style");
        Assert.assertEquals(1, htmlElements.size());
        Document normalDoc = Jsoup.parse(result.getNormal());
        Elements normalElements = normalDoc.select("style");
        Assert.assertEquals(0, normalElements.size());
        System.out.println("默认过滤后结果：\n" + result.getNormal());
        System.out.println("自定义html过滤结果：\n" + result.getHtml());
    }
```
- 测试结果，对不同字段实行了不同的过滤策略
```
默认过滤后结果：
<p>text</p>
自定义html过滤结果：
<style type="text/css"></style>
<p>text</p>
```