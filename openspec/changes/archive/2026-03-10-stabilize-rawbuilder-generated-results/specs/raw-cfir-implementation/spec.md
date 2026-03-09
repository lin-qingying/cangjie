## 新增需求

## 修改需求

### 需求:Raw CFIR 类型转换必须覆盖基础类型
Raw CFIR 构建阶段在处理类型引用时，必须将基础类型（如 `Int64`、`Unit`、`Bool`）转换为 `CfirBasicTypeRef`。基础类型身份必须在 RAW 阶段即可确定，禁止将其统一降级为错误类型引用、复用普通用户类型引用，或延后到 resolve 阶段再识别。

#### 场景:函数签名中的基础类型
- **当** 函数参数或返回值声明为基础类型
- **那么** Raw CFIR 渲染输出必须展示对应类型名称，不得出现 `Unsupported type element: CjBasicType`

#### 场景:RAW-only 构建即可确认基础类型
- **当** 仅执行 psi2cfir rawBuilder 而不执行 cfir-resolve
- **那么** 输出中基础类型引用必须已正确成型

### 需求:RawBuilder Golden 比对应由显式更新模式控制
RawBuilder 与 LazyBodies 测试中的 golden 文件更新必须通过显式开关控制。默认运行必须执行严格比对并失败，禁止自动改写 golden。

#### 场景:默认测试严格比对
- **当** 开发者未开启更新开关运行 rawBuilder 测试
- **那么** golden 不匹配时测试必须失败且不得修改期望文件

#### 场景:更新模式允许改写
- **当** 开发者显式开启更新开关运行 rawBuilder 测试
- **那么** 测试框架必须允许写回新的期望文件内容

### 需求:By-Stub LazyBodies 必须保证输入链路可用
By-stub 测试路径必须保证 `.cj` 输入具备正确 file type 与可用 stub，避免基础设施异常导致结果失真。

#### 场景:stub 路径稳定
- **当** 执行 by-stub lazyBodies 测试
- **那么** 测试必须满足 physical provider 与 stub 可用的前置条件

## 移除需求
