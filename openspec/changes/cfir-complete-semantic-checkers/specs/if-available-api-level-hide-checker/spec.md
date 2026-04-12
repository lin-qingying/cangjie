## 新增需求

### 需求:@IfAvailable 参数合法性检查
系统必须验证 @IfAvailable 注解的参数。

#### 场景:@IfAvailable 参数无名称
- **当** @IfAvailable 的第一个参数没有名称
- **那么** 必须报告 `IFAVAILABLE_ARG_NO_NAME` 错误

#### 场景:@IfAvailable 参数非字面量
- **当** @IfAvailable 的第一个参数不是字面量表达式
- **那么** 必须报告 `IFAVAILABLE_ARG_NOT_LITERAL` 错误

#### 场景:@IfAvailable 未知参数名
- **当** @IfAvailable 使用了不认识的参数名
- **那么** 必须报告 `IFAVAILABLE_UNKNOWN_ARG_NAME` 错误

#### 场景:APILevel 低于 19 不可用
- **当** APILevel 低于 19 时使用 @IfAvailable
- **那么** 必须报告 `IFAVAILABLE_LEVEL_LIMIT` 错误

### 需求:@APILevel 合法性检查
系统必须验证 @APILevel 注解的使用规范。

#### 场景:多个 @!APILevel
- **当** 声明上标注了多个 @!APILevel
- **那么** 必须报告 `APILEVEL_MULTI_ANNO` 错误

#### 场景:缺少命名参数
- **当** @!APILevel 缺少必要的命名参数
- **那么** 必须报告 `APILEVEL_MISSING_ARG` 警告

#### 场景:非字面量值
- **当** @!APILevel 的参数值不是字面量
- **那么** 必须报告 `ONLY_LITERAL_SUPPORT` 错误

#### 场景:引用更高 level 的声明
- **当** 代码引用了比当前作用域 APILevel 更高的声明
- **那么** 必须报告 `APILEVEL_REF_HIGHER` 错误

### 需求:@Hide 合法性检查
系统必须验证 @Hide 注解的使用规范。

#### 场景:多个 @!Hide
- **当** 声明上标注了多个 @!Hide
- **那么** 必须报告 `HIDE_MULTI_ANNOTATION` 错误

#### 场景:@!Hide 用在函数参数
- **当** @!Hide 注解用在函数参数上
- **那么** 必须报告 `HIDE_AT_FUNC_PARAM` 错误

#### 场景:缺少 @!Hide 标记
- **当** 要隐藏的声明未标记 @!Hide
- **那么** 必须报告 `HIDE_MISSING_HIDE` 错误

#### 场景:@!Hide 编译期不可见
- **当** @!Hide 注解在编译期不可见
- **那么** 必须报告 `HIDE_COMPILE_TIME_INVISIBLE` 错误
