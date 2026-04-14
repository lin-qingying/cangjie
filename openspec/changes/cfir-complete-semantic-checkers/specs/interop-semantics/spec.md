## 新增需求

### 需求:Interop 组全部诊断必须被覆盖
系统必须覆盖 `Interop` 组的全部诊断定义：`INVALID_CFUNC_RETURN_TYPE`。

#### 场景:CFunc 返回类型非法
- **当** interop 相关函数声明使用了不被允许的 CFunc 返回类型
- **那么** 必须报告 `INVALID_CFUNC_RETURN_TYPE`
