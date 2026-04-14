## 新增需求

### 需求:Initialization 组全部诊断必须被覆盖
系统必须覆盖 `Initialization` 组的全部诊断定义：`USED_BEFORE_INITIALIZATION`、`CLASS_UNINITIALIZED_FIELD`。

#### 场景:变量在初始化前被读取
- **当** 局部变量、属性或等价存储位置在确定初始化前被访问
- **那么** 必须报告 `USED_BEFORE_INITIALIZATION`

#### 场景:构造流程结束时字段仍未初始化
- **当** class 构造完成后仍存在未初始化字段
- **那么** 必须报告 `CLASS_UNINITIALIZED_FIELD`
