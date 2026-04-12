## 新增需求

### 需求:extend override 限制
系统必须禁止 extend 中 override 超类型的函数。

#### 场景:extend 中 override 函数
- **当** extend 体内尝试 override 被扩展类型超类型的函数
- **那么** 必须报告 `EXTEND_FUNCTION_CANNOT_OVERRIDDEN` 错误

### 需求:extend 成员遮蔽限制
系统必须禁止 extend 成员遮蔽被扩展类型的已有成员。

#### 场景:extend 成员遮蔽
- **当** extend 中定义的成员与被扩展类型的已有成员同名
- **那么** 必须报告 `EXTEND_MEMBER_CANNOT_SHADOW` 错误

### 需求:extend 非法成员检查
系统必须限制 extend 中只允许函数、属性和关联类型��

#### 场景:extend 中出现非法成员
- **当** extend 体内包含非函数、属性、关联类型的成员声明
- **那么** 必须报告 `EXTEND_ILLEGAL_MEMBER` 错误

### 需求:extend 检查顺序检查
系统必须验证 extend 的检查顺序可确定。

#### 场景:extend 检查顺序无法确定
- **当** extend 之间的依赖关系形成环路导致无法确定检查顺序
- **那么** 必须报告 `EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE` 错误

### 需求:导出 extend 依赖检查
系统必须检查导出的 extend 不依赖非导出 extend。

#### 场景:导出 extend 依赖非导出 extend
- **当** 导出的 extend 间接依赖非导出 extend 的函数
- **那么** 必须报告 `EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND` 错误

### 需求:@Java 类型 extend 限制
系统必须禁止 extend @Java 类型。

#### 场景:extend @Java 类型
- **当** extend 的目标是 @Java 注解的类型
- **那么** 必须报告 `EXTEND_A_JAVA_TYPE` 错误

### 需求:extend 导入接口限制
系统必须检查类型不能通过 extend 导入接口的特定限制。

#### 场景:类型 extend 导入接口
- **当** 类型尝试通过 extend 实现一个不允许导入的接口
- **那么** 必须报告 `TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE` 错误
