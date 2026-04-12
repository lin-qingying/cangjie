## 新增需求

### 需求:返回类型推断检查
系统必须检查函数返回类型的可推断性。

#### 场景:无法推断返回类型
- **当** 函数既无显式返回类型注解，且函数体无法推断出唯一返回类型
- **那么** 必须报告 `UNABLE_TO_INFER_RETURN_TYPE` 错误

### 需求:泛型函数类型参数推断检查
系统必须检查泛型函数的类型参数��推断性。

#### 场景:无法推断泛型函数类型参数
- **当** 调用泛型函数时无法从上下文推断出类型参数
- **那么** 必须报告 `UNABLE_TO_INFER_GENERIC_FUNC` 错误

### 需求:被调用对象合法性检查
系统必须验证被调用的���象是函数或构造器。

#### 场景:调用非函数对象
- **当** 对一个既不是函数也不是构造器的对象执行调用操作
- **那么** 必须报告 `INVALID_CALLED_OBJECT` 错误

### 需求:return 位置合法性检查
系统必须验证 return 语句的使用位置。

#### 场景:return 在函数体外
- **当** return 语句出现在函数体外部
- **那么** 必须报告 `INVALID_RETURN` 错误

#### 场景:return 在 static init 中
- **当** return 语句出现在 static init 块中
- **那么** 必须报告 `INVALID_RETURN_IN_STATIC_INIT` 错误

### 需求:subscript operator 合法性检查
系统必须验证 subscript operator `[]` 的声明规范。

#### 场景:subscript 赋值参数不合法
- **当** subscript operator 的赋值版本没有恰好一个名为 'value' 的命名参数
- **那么** 必须报告 `INVALID_SUBSCRIPT_ASSIGN_PARAMETER` 错误

#### 场景:subscript 缺少位置参数
- **当** subscript operator 没有至少一个位置参数作为下标
- **那么** 必须报告 `INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM` 错误

#### 场景:subscript 赋值返回非 Unit
- **当** subscript 赋值操作的返回类型不是 Unit
- **那么** 必须报告 `INVALID_SUBSCRIPT_ASSIGN_RETURN` 错误

### 需求:static 函数重载冲突检查
系统必须检查同名函数不能混合 static 和 non-static。

#### 场景:static 与 non-static 重载冲突
- **当** 同名函数存在 static 和 non-static 两种版本
- **那么** 必须报告 `STATIC_FUNCTION_OVERLOAD_CONFLICTS` 错误

### 需求:mut 函数引用限制
系统必须禁止 mut 函数被单独作为引用使用。

#### 场景:mut 函数作为引用
- **当** mut 函数被单独引用（非调用）
- **那么** 必须报告 `USE_MUTABLE_FUNC_ALONE` 错误

### 需求:unsafe 函数引用限制
系统必须禁止 unsafe 函数被作为名称引用。

#### 场景:unsafe 函数作为引用
- **当** unsafe 函数被引用而非调用
- **那么** 必须报告 `UNSAFE_FUNC_CAN_ONLY_BE_CALLED` 错误

### 需求:基本类型扩展歧义检查
系统必须检查基本类型扩展调用是否产生歧义。

#### 场景:基本类型扩展歧义
- **当** 对基本类型的方法调用匹配到多个 extend 中的候选
- **那么** 必须报告 `AMBIGUOUS_MATCH_PRIMITIVE_EXTEND` 错误

### 需求:默认参数限制检查
系统必须检查某些函数类型不能使用默认参数。

#### 场景:不允许默认参数的函数类型
- **当** 操作符函数或其他限制类型的函数声明了默认参数
- **那么** 必须报告 `CANNOT_HAVE_DEFAULT_PARAM` 错误

### 需求:trailing lambda 类型检查
系统必须验证 trailing lambda 参数的类型。

#### 场景:trailing lambda 用于非函数类型参数
- **当** trailing lambda 语法用于一个非函数类型的参数
- **那么** 必须报告 `TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION` 错误

### 需求:lambda 参数类型注解检查
系统必须要求 lambda 表达式的参数提供类型注解。

#### 场景:lambda 参数缺少类型注解
- **当** lambda 表达式的参数没有类型注解且无法从上下文推断
- **那么** 必须报告 `LAMBDA_MUST_HAVE_TYPE_ANNOTATION` 错误

### 需求:捕获可变变量的闭包使用限制
系统必须检查捕获可变变量的闭包的使用方式。

#### 场景:捕获可变变量的闭包未直接调用
- **当** 一个捕获了可变变量的闭包被存储或传递而非直接调用
- **那么** 必须报告 `USE_FUNC_CAPTURE_VAR_ALONE` 错误
