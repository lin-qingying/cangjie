## 新增需求

### 需求:Effects 组全部诊断必须被覆盖
系统必须覆盖 `Effects` 组的全部诊断定义：`EFFECTS_FEATURE_DISABLED`、`COMMAND_INCOMPATIBLE_TYPE`、`COMMAND_HANDLE_TYPE_ERROR`、`IMPLICIT_RESUME_OUTSIDE_HANDLER`、`RESUME_NO_WITH`、`RESUME_THROWING_MISMATCH_TYPE`、`MISMATCHING_HANDLE_BLOCK`。

#### 场景:effects 特性未启用
- **当** 代码使用 effect 相关语法，但编译配置未启用对应特性
- **那么** 必须报告 `EFFECTS_FEATURE_DISABLED`

#### 场景:command 或 handle 类型不兼容
- **当** perform、handle、command pattern 的类型与 effect 语义不匹配
- **那么** 必须分别报告 `COMMAND_INCOMPATIBLE_TYPE`、`COMMAND_HANDLE_TYPE_ERROR` 或 `MISMATCHING_HANDLE_BLOCK`

#### 场景:resume 语义非法
- **当** `resume` 出现在 handler 外、缺少 `with`，或抛出值类型与 resumption 类型不匹配
- **那么** 必须分别报告 `IMPLICIT_RESUME_OUTSIDE_HANDLER`、`RESUME_NO_WITH`、`RESUME_THROWING_MISMATCH_TYPE`
