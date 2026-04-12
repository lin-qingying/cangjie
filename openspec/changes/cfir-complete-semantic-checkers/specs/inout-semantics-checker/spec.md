## 新增需求

### 需求:inout CString/零大小类型限制
系统必须禁止 inout 修饰 CString 或零大小类型。

#### 场景:inout CString 或零大小类型
- **当** inout 表达式的类型是 CString 或零大小类型
- **那么** 必须报告 `INOUT_MODIFY_CSTRING_OR_ZEROSIZED` 错误

### 需求:inout CType 约束检查
系统必须确保 inout 表达式的类型满足 CType 约束。

#### 场景:inout 非 CType
- **当** inout 表达式的类型不满足 CType 约束
- **那么** 必须报告 `INOUT_MODIFY_NON_CTYPE` 错误

### 需求:inout var 变量约束
系统必须确保 inout 只修饰 var 变量。

#### 场景:inout 非 var 变量
- **当** inout 修饰了非 var 变量（如 let 变量）
- **那么** 必须报告 `INOUT_MUST_BE_VAR_VARIABLE` 错误

### 需求:inout 堆变量约束
系统必须禁止 inout 修饰来自 class 实例的变量。

#### 场景:inout 堆变量
- **当** inout 修饰的变量直接或间接来自 class 实例（堆分配）
- **那么** 必须报告 `INOUT_MODIFY_HEAP_VARIABLE` 错误

### 需求:inout 使用上下文限制
系统必须确保 inout 只在 CFunc 调用中使用。

#### 场景:inout 在非 CFunc 调用中
- **当** inout 参数出现在非 CFunc 的函数调用中
- **那么** 必须报告 `INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING` 错误

### 需求:inout 参数类型匹配检查
系统必须确保 inout 参数类型匹配。

#### 场景:inout 参数类型不匹配
- **当** inout 参数的类型与形参声明的指针类型不匹配
- **那么** 必须报告 `INOUT_MISMATCH` 错误
