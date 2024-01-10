### 接入说明

该组件以AOP为底层支持，可以过滤Controller层返回值的字段，在列级上达到数据过滤效果。

```
<!--数据字段权限组件-->
<dependency>
    <groupId>org.example</groupId>
    <artifactId>data-field-permission-boot-start</artifactId>
    <version>1.0</version>
</dependency>
```

### 使用说明

#### 实现org.example.core.DataPermissionProvider接口

- **getDataPermissionMapping**

  该方法提供权限和数据及数据字段的对应关系

- **getCurrentDataPermissions**

  获取当前方法调用者的数据权限

#### 注意事项

该组件已配置了AOP的切面为：

```
@Around("execution(* *.*Controller.*(..))")
```

目标控制器的命名必须符合该配置