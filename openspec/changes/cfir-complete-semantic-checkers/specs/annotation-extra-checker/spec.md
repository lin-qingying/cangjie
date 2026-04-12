## 新增需求

### 需求:@Annotation 参数约束检查
系统必须验证 @Annotation 的参数规范。

#### 场景:@Annotation 参数不是 target
- **当** @Annotation 的参数名称不是 'target'
- **那么** 必须报告 `ANNOTATION_ARG_TARGET` 错误

#### 场景:@Annotation 参数不是数组字面量
- **当** @Annotation 的 target 参数不是数组字面量
- **那么** 必须报告 `ANNOTATION_ARG_TARGET_ARRAY_LIT` 错误

### 需求:非 public 注解可见性检查
系统必须对非 public 的 @Annotation 类发出警告。

#### 场景:@Annotation 类不是 public
- **当** 标记了 @Annotation 的类可见性不是 public
- **那么** 必须报告 `ANNOTATION_NON_PUBLIC` 警告

### 需求:自定义注解位置检查
系统必须验证自定义注解的使用位置。

#### 场景:不允许使用自定义注解
- **当** 在不允许自定义注解的位置使用了自定义注解
- **那么** 必须报告 `ANNOTATION_CUSTOM_PLACE` 错误

### 需求:注解参数合法性检查
系统必须验证注解的参数个数和范围。

#### 场景:注解参数个数错误
- **当** 注解的参数个数与声明不匹配
- **那么** 必须报告 `ANNOTATION_ERROR_ARG_NUM` 错误

#### 场景:注解参数范围错误
- **当** 注解的参数值不在支持的范围内
- **那么** 必须报告 `ANNOTATION_ERROR_ARG_RANGE` 错误

### 需求:注解目标限制检查
系统必须验证注解只修饰合法目标。

#### 场景:注解目标不合法
- **当** 注解被用于不支持的目标（如在变量上使用只能修饰函数的注解）
- **那么** 必须报告 `ANNOTATION_ERROR_OBJECT` 错误

### 需求:Java 互操作注解位置限制
系统必须验证 Java 互操作注解的使用规范。

#### 场景:JFFI 上下文不能使用注解
- **当** 在 Java 互操作上下文中使用了不允许的注解
- **那么** 必须报告 `CANNOT_USE_ANNOTATION_JFFI` 错误

#### 场景:JFFI 注解目标不匹配
- **当** Java 互操作注解被用于不匹配的目标
- **那么** 必须报告 `ANNOTATION_NOT_APPLICABLE_JFFI` 错误
