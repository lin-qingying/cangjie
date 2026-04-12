## 新增需求

### 需求:泛型类型替换一致性检查
系统必须验证泛型类型替换的一致性。

#### 场景:泛型类型替换不一致
- **当** 同一类型参数在不同上下文中被替换为不一致的类型
- **那么** 必须报告 `GENERIC_TYPE_INCONSISTENT` 错误

### 需求:泛型参数个数检查
系统必须验证泛型参数个数与声明匹配。

#### 场景:泛型参数个数不匹配
- **当** 泛型实例化时提供的类型参数个数与声明不一致
- **那么** 必须报告 `GENERIC_ARGUMENT_NO_MATCH` 错误

### 需求:泛型约束宽松性检查
系统必须禁止子类型约束比父类更宽松。

#### 场景:子类型约束宽松
- **当** 子类型的泛型约束比父类型声明的约束更宽松
- **那么** 必须报告 `GENERIC_CONSTRAINT_NOT_LOOSER` 错误

### 需求:泛型实例化歧义检查
系统必须检查泛型实例化是否导致函数歧义。

#### 场景:泛型实例化导致歧义
- **当** 泛型实例化后产生多个同签名的函数候选
- **那么** 必须报告 `GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS` 错误

### 需求:泛型参数递归绑定检查
系统必须检查泛型参数的递归引用。

#### 场景:泛型参数直接递归
- **当** 泛型参数的上界直接引用自身
- **那么** 必须报告 `GENERIC_PARAM_DIRECTLY_RECURSIVE` 错误

#### 场景:泛型参数间接递归
- **当** 泛型参数的上界通过与类无关的路径递归引用自身
- **那么** 必须报告 `GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY` 错误

### 需求:泛型上界类型限制
系统必须要求泛型参数的上界是 class 或 interface。

#### 场景:上界非 class/interface
- **当** 泛型参数的上界不是 class 或 interface 类型
- **那么** 必须报告 `UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE` 错误

### 需求:泛型 static 成员访问限制
系统必须禁止 static 成员依赖泛型参数。

#### 场景:static 成员依赖泛型参数
- **当** 在 Java 互操作类型中 static 成员的类型依赖泛型参数
- **那么** 必须报告 `GENERIC_STATIC_ACCESS` 错误

### 需求:基本类型泛型参数限制
系统必须禁止基本类型作为 @Java 泛型参数。

#### 场景:基本类型作为 Java 泛型参数
- **当** 基本类型（如 Int32）被用作 @Java 泛型的类型参数
- **那么** 必须报告 `PRIMITIVE_TYPE_AS_GENERICS_ARG` 错误
