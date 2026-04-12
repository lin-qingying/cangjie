## 新增需求

### 需求:static 成员未初始化检查
系统必须检查 static 成员变量是否已初始化。

#### 场景:static 成员未初始化
- **当** static 成员变量没有初始化表达式且未在 static init 中初始化
- **那么** 必须报告 `TYPE_UNINITIALIZED_STATIC_FIELD` 错误

### 需求:finalizer 实例成员使用限制
系统必须禁止在 finalizer 中使用实例���员。

#### 场景:finalizer 中使用实例成员
- **当** 在 finalizer 函数体中访问实例成员函数或属性
- **那么** 必须报告 `INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER` 错误

### 需求:sealed 修饰符约束
系统必须确保 sealed 只用于抽象类。

#### 场景:非抽象类使用 sealed
- **当** 一个非抽象类被标记为 sealed
- **那么** 必须报告 `NON_ABSTRACT_CLASS_CANNOT_BE_SEALED` 错误

### 需求:static 变量泛型参数依赖检查
系统必须禁止 static 变量依赖泛型参数。

#### 场景:static 变量使用泛型参数
- **当** static 变量的类型引用了所在类的泛型参数
- **那么** 必须报告 `STATIC_VARIABLE_USE_GENERIC_PARAMETER` 错误

### 需求:@C struct 接口实现限制
系统必须禁止 @C struct 实现接口。

#### 场景:@C struct 实现接口
- **当** 标记为 @C 的 struct 尝试实现接口
- **那么** 必须报告 `CSTRUCT_CANNOT_IMPL_INTERFACES` 错误

### 需求:同名 private 声明导出限制
系统必须禁止同时导出两个同名的 private 声明。

#### 场景:同名 private 声明导出
- **当** 两个同名的 private 声明被同时导出
- **那么** 必须报告 `EXPORT_SAME_PRIVATE_DECL` 错误
