## 新增需求

### 需求:CallResolution 组全部诊断必须被覆盖
系统必须覆盖 `CallResolution` 组的全部诊断定义：`NO_VALUE_FOR_PARAMETER`、`TOO_MANY_ARGUMENTS`、`NAMED_PARAMETER_NOT_FOUND`、`ARGUMENT_PASSED_TWICE`、`NAMED_ARGUMENTS_NOT_ALLOWED`、`MIXING_NAMED_AND_POSITIONAL_ARGUMENTS`、`NEED_NAMED_ARGUMENT`、`AMBIGUOUS_CONSTRUCTOR_CALL`、`AMBIGUOUS_FUNCTION_CALL`、`RECURSIVE_CONSTRUCTOR_CALL`、`ILLEGAL_THIS_OR_SUPER_CALL`、`EXPLICIT_SUPER_CALL_REQUIRED`、`INVALID_LOOP_CONTROL`。

#### 场景:参数绑定失败
- **当** 调用过程中出现缺参、多参、命名参数不存在、重复传参、命名参数顺序错误或必须具名却未具名
- **那么** 必须分别报告对应的 `CallResolution` 诊断

#### 场景:候选选择发生歧义
- **当** 构造器调用或函数调用存在多个同等可接受候选且无法选出唯一目标
- **那么** 必须分别报告 `AMBIGUOUS_CONSTRUCTOR_CALL` 或 `AMBIGUOUS_FUNCTION_CALL`

#### 场景:构造器委托或 super 调用非法
- **当** 构造器委托形成递归、`this` / `super` 调用位置非法，或父类要求显式 `super(...)`
- **那么** 必须分别报告 `RECURSIVE_CONSTRUCTOR_CALL`、`ILLEGAL_THIS_OR_SUPER_CALL`、`EXPLICIT_SUPER_CALL_REQUIRED`

#### 场景:循环控制语句脱离循环体
- **当** `break` 或 `continue` 出现在循环体之外
- **那么** 必须报告 `INVALID_LOOP_CONTROL`

### 需求:CallResolution 诊断必须由 resolve 管线负责
系统必须在 resolve 阶段完成参数映射与候选选择诊断，不得在 checker 阶段重做一套调用绑定逻辑。

#### 场景:调用绑定错误已可在 resolve 判断
- **当** resolve 已具备完整候选与参数映射信息
- **那么** 必须在 resolve 阶段直接发射 `CallResolution` 诊断
- **并且** 不得依赖 checker 事后兜底
