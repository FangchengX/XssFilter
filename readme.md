## AOP+Jsoup 实现灵活xss防御

切面切入所有的接口， 对post接口中加了@Valid注解的body中的String类型字段进行xss过滤。通过自定义注解实现不同的过滤策略。

增加不同的防御策略只需要增加一组白名单+注解。