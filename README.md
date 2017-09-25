# mybatis-generator-plugins

## 使用 Maven

```xml
<dependency>
    <groupId>com.gulucat</groupId>
    <artifactId>generator-plugins</artifactId>
    <version>1.0</version>
</dependency>
```

## 插件说明

### 1. FieldNameConstantPlugin - 生成常量字段插件

#### 1.1 插件描述

例如您的实体类中有个id字段，使用此插件则会生成一个常量：

    public static String ID = "id";

结合 [Mapper](https://github.com/abel533/Mapper) 插件，可以实现如下的功能：

    Condition condition = new Condition(YourEntity.class);
    Criteria criteria = condition.createCriteria();
    criteria.andEqualTo(YourEntity.ID, id);

避免在需要的地方直接使用字符串而不好维护。

#### 1.2 使用方法

在 mybatis generator 配置文件的context节点里，加上plugin配置：

```xml
    <!-- 生成model的字段名称常量插件 -->
    <plugin type="com.gulucat.generator.plugins.FieldNameConstantPlugin">
        <property name="constantType" value="field" />
    </plugin>
```

#### 1.3 可选参数

- <b>constantType</b>： 可选值有"field", "innerClass", "enum"。 默认值为"field"。

- <b>fieldName</b>： 当`constantType`设置为`innerClass`或者`field`时，需要用到该值。默认值为"Field"。

- <b>prefix</b>： 常量字段前缀。默认为空。

- <b>suffix</b>： 常量字段后缀。默认为空。


当`constantType`设置成`field`时，实体字段直接生成一个静态成员常量，示例如下：

    public static String ID = "id";
    public static String USER_NAME = "userName";

当`constantType`设置成`innerClass`时，实体字段被包裹在一个内部类中，示例如下：

    public static class Field {
        public static String ID = "id";
        public static String USER_NAME = "userName";
    }

内部类的名称通过属性`fieldName`来进行设置。

当`constantType`设置成`enum`时，实体字段被包裹在一个枚举中，示例如下：

    public static enum Field {
        ID, USER_NAME
    }

枚举的名称通过属性`fieldName`来进行设置。
