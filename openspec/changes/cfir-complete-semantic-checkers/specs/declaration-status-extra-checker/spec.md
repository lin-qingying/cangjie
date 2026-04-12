## 新增需求

### 需求:参数命名一致性检查
系统必须检查 override/redef 的形参与父声明命名参数语义一致。

#### 场景:参数命名语义不一致
- **当** override/redef 的形参命名参数语义与父声明不一致（如父声明要求命名参数但子声明未保持）
- **那么** 必须报告 `PARAM_NAMED_MISMATCHED` 错误

### 需求:override static 检查
系统必须检查 override 时 static 属性的一致性。

#### 场景:override static 成员为实例成员或反之
- **当** 子声明尝试将 static 成员 override 为实例成员，或反之
- **那么** 必须报告 `OVERRIDE_STATIC_ERROR` 错误

### 需求:redef 实例成员检查
系统必须检查 redef 时实例属性的一致性。

#### 场景:redef 实例成员不一致
- **当** redef 的声明与父声明的实例/static 属性不一致
- **那么** 必须报告 `REDEF_INSTANCE_ERROR` 错误

### 需求:操作符参数个数检查
系统必须验证操作符声明的参数个数。

#### 场景:操作符参数个数不正确
- **当** 操作符函数的参数个数与预期不匹配
- **那么** 必须报告 `INVALID_OPERATOR_PARAMETER_COUNT` 错误

### 需求:未使用导入检查
系统必须检测未使用的 import 语句。

#### 场景:import 语句未使用
- **当** import 语句引入的符号在文件中从未被使用
- **那么** 必须报告 `UNUSED_IMPORT` 警告
