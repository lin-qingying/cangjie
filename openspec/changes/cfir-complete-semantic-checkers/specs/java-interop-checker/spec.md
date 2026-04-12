## 新增需求

### 需求:@Java 类型使用约束检查
系统必须验证 @Java 类型的使用上下文。

#### 场景:@Java["ext"] 类型使用错误
- **当** @Java["ext"] 类型在非 @Java["ext"] 声明中使用
- **那么** 必须报告 `JAVA_INCORRECT_USE_BETWEEN_TYPES` 错误

### 需求:JType 兼容性检查
系统必须验证 @Java 声明中类型满足 JType 约束。

#### 场景:@Java 声明中非 JType
- **当** @Java 声明中的参数/返回/字段类型不满足 JType 约束
- **那么** 必须报告 `JAVA_NON_JTYPE` 错误

#### 场景:@Java 声明中 Unit 类型
- **当** @Java 声明中使用了 Unit 类型（不合法的位置）
- **那么** 必须报告 `JAVA_INVALID_UNIT` 错误

### 需求:@Java 继承约束检查
系统必须验证 @Java 类型的继承规则。

#### 场景:非 ext 类型继承 ext 类型
- **当** 非 @Java["ext"] 类型尝试从 @Java["ext"] 类型继承
- **那么** 必须报告 `JAVA_APP_INHERIT_EXT` 错误

### 需求:@Java 不支持的声明检查
系统必须检查 @Java 注解类型中不支持的声明。

#### 场景:@Java 中不支持的声明
- **当** @Java 注解的类型中包含不支持的成员声明
- **那么** 必须报告 `JAVA_UNSUPPORTED_DECL` 错误

### 需求:@Java 注解缺失检查
系统必须检查应有 @Java 注解但缺失的情况。

#### 场景:缺少 @Java 注解
- **当** 声明应该标记 @Java 但未标记
- **那么** 必须报告 `MISSING_JAVA_INTEROP_ANNOTATION` 错误

### 需求:Java mirror 类型约束检查
系统必须验证 @JavaMirror/@JavaImpl 的类型约束。

#### 场��:mirror 构造器参数非 mirror 类型
- **当** @JavaMirror 构造器的参数类型不是 @JavaMirror 类型
- **那么** 必须报告 `JAVA_MIRROR_CTOR_ARG_MUST_BE_JAVA_MIRROR` 错误

#### 场景:mirror 方法参数非 mirror 类型
- **当** @JavaMirror 方法的参数类型不是 @JavaMirror 类型
- **那么** 必须报告 `JAVA_MIRROR_METHOD_ARG_MUST_BE_JAVA_MIRROR` 错误

#### 场景:mirror 属性非 mirror 类型
- **当** @JavaMirror 声明的属性类型不是 @JavaMirror 类型
- **那么** 必须报告 `JAVA_MIRROR_PROP_MUST_BE_JAVA_MIRROR` 错误

### 需求:Java mirror 继承约束检查
系统必须验证 @JavaMirror/@JavaImpl 的继承约束。

#### 场景:mirror 子类型缺少注解
- **当** 继承 @JavaMirror 的子类型未标记 @JavaMirror 或 @JavaImpl
- **那么** 必须报告 `JAVA_MIRROR_SUBTYPE_MUST_BE_ANNOTATED` 错误

#### 场景:mirror 继承纯仓颉类型
- **当** @JavaMirror 类型继承了非 Java 互操作的纯仓颉类型
- **那么** 必须报告 `JAVA_MIRROR_CANNOT_INHERIT_PURE_CANGJIE_TYPE` 错误

#### 场景:impl 必须继承 mirror
- **当** @JavaImpl 类型未继承 @JavaMirror 类型
- **那么** 必须报告 `JAVA_MIRROR_SUBTYPE_ANNO_MUST_INHERIT_MIRROR` 错误

### 需求:Java 互操作变量/泛型类型限制
系统必须限制 Java 互操作类型的存储和泛型实例化。

#### 场景:存储 Java 互操作类型
- **当** 变量的类型是 Java 互操作类型
- **那么** 必须报告 `VARIABLE_OF_JAVA_TYPE` 错误

#### 场景:Java 互操作类型实例化泛型
- **当** Java 互操作类型被用于泛型实例化
- **那么** 必须报告 `GENERIC_PARAMETER_OF_JAVA_TYPE` 错误

### 需求:Java 互操作功能支持检查
系统必须对暂不支持的 Java 互操作功能报告错误。

#### 场景:Java 互操作功能暂不支持
- **当** 使用了暂不支持的 Java 互操作功能
- **那么** 必须报告 `JAVA_INTEROP_NOT_SUPPORTED` 错误
