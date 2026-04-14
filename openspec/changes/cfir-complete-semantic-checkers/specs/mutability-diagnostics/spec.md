## 新增需求

### 需求:Mutability 组全部诊断必须被覆盖
系统必须覆盖 `Mutability` 组的全部诊断定义：`CANNOT_MODIFY_VAR`、`IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION`。

#### 场景:不可变上下文修改变量
- **当** 当前语义上下文不允许修改目标变量却发生写入
- **那么** 必须报告 `CANNOT_MODIFY_VAR`

#### 场景:不可变成员函数调用 mut 成员函数
- **当** immutable 成员函数试图调用当前实例上的 mutable 成员函数
- **那么** 必须报告 `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION`
