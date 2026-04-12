## 新增需求

### 需求:resumption 类型合法性检查
系统必须验证 resumption 类型扩展 effect.Resumption。

#### 场景:resumption 类型不合法
- **当** handle clause 中的 resumption 类型不扩展 effect.Resumption
- **那么** 必须报告 `RESUMPTION_HANDLE_TYPE_ERROR` 错误

### 需求:resumption 返回类型匹配检查
系统必须确保 resumption 返回类型与 try block 匹配。

#### 场景:resumption 返回类型不匹配
- **当** resumption 的返回类型与 try block 的类型不匹配
- **那么** 必须报告 `RESUMPTION_INCORRECT_RETURN_TYPE` 错误

### 需求:command-resumption 类型匹配检查
系统必须确保 resumption 参数类型与 command 结果类型匹配。

#### 场景:command-resumption 类型不匹配
- **当** resumption 的参数类型与 command 的结果类型不匹配
- **那么** 必须报告 `COMMAND_RESUMPTION_MISMATCH` 错误

### 需求:resume resumption 类型检查
系统必须确保 resume 的 resumption 类型是 core.Resumption<T>。

#### 场景:resume 的 resumption 类型不正确
- **当** resume 表达式的 resumption 类型不是 core.Resumption<T>
- **那么** 必须报告 `RESUME_WRONG_RESUMPTION_TYPE` 错误

### 需求:try/handle block return 限制
系统必须禁止在 try/handle block 中使用 return。

#### 场景:try/handle 中 return
- **当** return 语句出现在 try/handle block 中
- **那么** 必须报告 `RETURN_IN_TRY_HANDLE_BLOCK` 错误

### 需求:无用 command 类型检查
系统必须对无用的 command 类型发出警告。

#### 场景:无用 command 类型
- **当** 声明的 command 类型在 handle 中从未被使用
- **那么** 必须报告 `USELESS_COMMAND_TYPE` 警告
