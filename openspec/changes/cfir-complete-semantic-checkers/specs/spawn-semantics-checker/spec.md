## 新增需求

### 需求:spawn 参数合法性检查
系统必须验证 spawn 的参数类型。

#### 场景:spawn 参数无效
- **当** spawn 的参数是自定义 ThreadContext 类型（非系统允许的类型）
- **那么** 必须报告 `SPAWN_ARG_INVALID` 错误

### 需求:spawn 参数效果检查
系统必须对不生效的 spawn 参数发出警告。

#### 场景:spawn 参数不生效
- **当** spawn 的参数在当前后端不生效
- **那么** 必须报告 `SPAWN_ARG_NO_EFFECT` 警告
