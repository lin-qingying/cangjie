## 新增需求

### 需求:@Deprecated 调用检查
系统必须检测对 @Deprecated 标记声明的调用。

#### 场景:调用 deprecated 声明（错误级别）
- **当** 调用了标记为 @Deprecated 且级别为 error 的声明
- **那么** 必须报告 `DEPRECATED_ERROR` 错误

#### 场景:调用 deprecated 声明（警告级别）
- **当** 调用了标记为 @Deprecated 且级别为 warning 的声明
- **那么** 必须报告 `DEPRECATED_WARNING` 警告

### 需求:@Deprecated 严格度继承约束
系统必须禁止继承者减弱 @Deprecated 的严格度。

#### 场景:弱化 deprecation 严格度
- **当** 子类对 @Deprecated 标记的成员设置了更低的弃用严格度
- **那么** 必须报告 `DEPRECATION_WEAKENING` 错误

### 需求:override 成员 @Deprecated 一致性
系统必须检查 override 成员是否需要标记 @Deprecated。

#### 场景:override 缺少 @Deprecated（错误级别）
- **当** 被覆盖的父成员标记了 @Deprecated（error），但 override 未标记
- **那么** 必须报告 `DEPRECATION_OVERRIDE_ERROR` 错误

#### 场景:override 缺少 @Deprecated（警告级别）
- **当** 被覆盖的父成员标记了 @Deprecated（warning），但 override 未标记
- **那么** 必须报告 `DEPRECATION_OVERRIDE_WARNING` 警告

### 需求:redef 成员 @Deprecated 一致性
系统必须检查 redef 成员是否需要标记 @Deprecated。

#### 场景:redef 缺少 @Deprecated（错误级别）
- **当** 被重定义的父成员标记了 @Deprecated（error），但 redef 未标记
- **那么** 必须报告 `DEPRECATION_REDEF_ERROR` 错误

#### 场景:redef 缺少 @Deprecated（警告级别）
- **当** 被重定义的父成员标记了 @Deprecated（warning），但 redef 未标记
- **那么** 必须报告 `DEPRECATION_REDEF_WARNING` 警告
