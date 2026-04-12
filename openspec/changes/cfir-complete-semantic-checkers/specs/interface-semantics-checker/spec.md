## 新增需求

### 需求:接口未实现 static 成员调用检查
系统必须检查接口的 static 调用中包含未实现的 static 成员。

#### 场景:接口 static 调用含未实现成员
- **当** 通过接口类型执行 static 调用，但该接口包含未实现的 static 函数或属性
- **那么** 必须报告 `INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL` 错误
