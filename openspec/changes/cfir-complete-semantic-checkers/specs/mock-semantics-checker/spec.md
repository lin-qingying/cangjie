## 新增需求

### 需求:Mock 功能启用检查
系统必须验证 Mock 功能的启用状态。

#### 场景:Mock 功能已禁用
- **当** Mock 功能被编译选项禁用
- **那么** 必须报告 `MOCK_DISABLED` 错误

#### 场景:Mock 不在测试模式
- **当** Mock 功能在非测试模式下使用
- **那么** 必须报告 `MOCK_NOT_IN_TEST_MODE` 错误

### 需求:Mock 类型约束检查
系统必须验证 Mock 的目标类型。

#### 场景:Mock 非 class/interface
- **当** 尝试 mock 非 class 或 interface 类型
- **那么** 必须报告 `MOCK_UNSUPPORTED_TYPE` 错误

### 需求:Mock static/top-level 声明约束
系统必须验证被 mock 的 static/top-level 声明的约束。

#### 场景:Mock 不合法的 static 声明
- **当** 尝试 mock 的 static/top-level 声明是 private/local/constant/constructor
- **那么** 必须报告 `MOCK_WRONG_STATIC_DECL` 错误

### 需求:Mock 包兼容性检查
系统必须验证目标包是否以 mock-compatible 方式编译。

#### 场景:目标不支持 mocking
- **当** 目标声明所在的包未以 mock-compatible 方式编译
- **那么** 必须报告 `MOCK_DOESNT_SUPPORT_MOCKING` 错误

### 需求:Mock @Frozen 兼容性检查
系统必须处理 Mock 与 @Frozen 的兼容性。

#### 场景:Mock @Frozen 声明
- **当** 尝试 mock 标记了 @Frozen 的声明
- **那么** 必须报告 `MOCK_FROZEN_UNSUPPORTED` 错误

#### 场景:createMock/createSpy 需要 @Frozen
- **当** createMock/createSpy 的泛型包装函数缺少 @Frozen 注解
- **那么** 必须报告 `MOCK_FROZEN_REQUIRED` 错误
