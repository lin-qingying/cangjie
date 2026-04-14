## 新增需求

### 需求:GenericAccess 组全部诊断必须被覆盖
系统必须覆盖 `GenericAccess` 组的全部诊断定义：`GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS`、`GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS`。

#### 场景:通过类型参数访问不存在的成员
- **当** 通过类型参数接收者访问其所有上界中都不存在的成员
- **那么** 必须报告 `GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS`

#### 场景:通过类型参数调用不存在的方法
- **当** 通过类型参数接收者调用其所有上界中都不存在的方法
- **那么** 必须报告 `GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS`
