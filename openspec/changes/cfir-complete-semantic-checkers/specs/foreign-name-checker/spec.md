## 新增需求

### 需求:@ForeignName override 位置约束
系统必须禁止 @ForeignName 出现在被 override 的声明上。

#### 场景:@ForeignName 在 override 声明上
- **当** 被 override 的子声明标记了 @ForeignName
- **那么** 必须报告 `FOREIGN_NAME_APPEARED_IN_CHILD` 错误

### 需求:@ForeignName 注解冲突检查
系统必须检查 @ForeignName 与其他注解的冲突。

#### 场景:@ForeignName 注解冲突
- **当** @ForeignName 与声明上的其他注解发生冲突
- **那么** 必须报告 `FOREIGN_NAME_CONFLICTING_ANNOTATION` 错误

#### 场景:@ForeignName 派生注解冲突
- **当** @ForeignName 的派生名与其他注解的名称冲突
- **那么** 必须报告 `FOREIGN_NAME_CONFLICTING_DERIVED_ANNOTATION` 错误
