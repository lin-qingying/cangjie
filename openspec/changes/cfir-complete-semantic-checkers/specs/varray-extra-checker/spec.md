## 新增需求

### 需求:VArray 构造器参数个数检查
系统必须确保 VArray 构造器只接受一个参数。

#### 场景:VArray 构造器参数个数不匹配
- **当** VArray 构造器被调用时传入非一个参数
- **那么** 必须报告 `VARRAY_ARGS_NUMBER_MISMATCH` 错误

### 需求:VArray subscript 约束检查
系统必须确保 VArray 只接受一个 Int64 类型的下标。

#### 场景:VArray 下标个数或类型不匹配
- **当** VArray 的 subscript 操作使用非一个 Int64 类型的下标
- **那么** 必须报告 `VARRAY_SUBSCRIPT_NUM` 错误

### 需求:VArray CFunc 返回限制
系统必须禁止 CFunc 的返回类型为 VArray。

#### 场景:CFunc 返回 VArray
- **当** CFunc 声明的返回类型是 VArray
- **那么** 必须报告 `VARRAY_IN_CFUNC` 错误

### 需求:VArray 元素类型限制
系统必须检查 VArray 元素类型不包含引用类型。

#### 场景:VArray 包含引用类型
- **当** VArray 直接或间接包含引用类型
- **那么** 必须报告 `VARRAY_ARG_TYPE_WITH_REFTYPE` 错误
