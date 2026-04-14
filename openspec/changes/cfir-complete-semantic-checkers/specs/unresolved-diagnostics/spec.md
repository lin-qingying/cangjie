## 新增需求

### 需求:Unresolved 组全部诊断必须被覆盖
系统必须覆盖 `Unresolved` 组的全部诊断定义：`UNRESOLVED_REFERENCE`、`INVALID_BINARY_OPERATOR`、`NO_MATCHING_OPERATOR_INVOKE`。

#### 场景:引用未解析
- **当** 名称引用、成员引用或类型引用无法解析到合法目标
- **那么** 必须报告 `UNRESOLVED_REFERENCE`

#### 场景:二元运算符非法
- **当** 代码使用了非法二元运算符，或当前操作数类型上不存在匹配的 operator invoke
- **那么** 必须分别报告 `INVALID_BINARY_OPERATOR` 或 `NO_MATCHING_OPERATOR_INVOKE`

### 需求:Unresolved 诊断必须由引用解析负责
系统必须在 unresolved / reference resolution 管线中直接产出这些诊断。

#### 场景:未解析事实只在解析阶段可确定
- **当** 诊断依赖候选搜索失败或操作符查找失败
- **那么** 必须在 resolve 阶段发射 `Unresolved` 诊断
- **并且** 不得由 checker 事后猜测
