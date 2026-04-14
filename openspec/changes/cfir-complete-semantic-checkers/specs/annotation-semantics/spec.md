## 新增需求

### 需求:Annotation 组全部诊断必须被覆盖
系统必须覆盖 `Annotation` 组的全部诊断定义：`ANNOTATION_NO_CONST_INIT`。

#### 场景:注解参数不是常量初始化
- **当** 注解参数需要常量初始化但实际表达式不是 const-init
- **那么** 必须报告 `ANNOTATION_NO_CONST_INIT`
