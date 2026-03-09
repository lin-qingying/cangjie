## 新增需求

### 需求:RawBuilder golden 更新必须显式受控
RawBuilder 相关测试在默认执行模式下必须只进行内容比较，禁止在断言失败时自动改写期望文件。仅当显式更新开关开启时，系统才必须允许写回 golden 文件。

#### 场景:默认模式不改写
- **当** 开发者运行 `:cfir:raw-cfir:psi2cfir:test` 且未开启更新开关
- **那么** 测试框架必须在不匹配时失败并保留原始 golden 文件内容不变

#### 场景:更新模式可改写
- **当** 开发者显式开启更新开关运行同一测试任务
- **那么** 测试框架必须允许将实际输出写回对应 golden 文件

### 需求:CjBasicType 必须转换为可用 Raw CFIR 类型引用
PSI 到 Raw CFIR 的类型转换必须支持 `CjBasicType`，并且基础类型身份必须在 RAW 阶段即可确定。系统必须使用 `CfirBasicTypeRef` 表达基础类型，禁止将基础类型默认降级为 `Unsupported type element` 错误类型。

#### 场景:基础类型参数与返回值
- **当** 输入函数签名包含 `Int64`、`Unit` 等基础类型
- **那么** rawBuilder 输出中的基础类型必须以 `CfirBasicTypeRef` 语义呈现，且禁止出现 `R|ERROR: Unsupported type element: CjBasicType|`

#### 场景:禁止延后到 resolve 才识别基础类型
- **当** 执行仅 RAW 构建与渲染（未进入 resolve）
- **那么** 基础类型必须已被正确识别并写入 Raw CFIR 输出

#### 场景:lazyBodies 输出一致性
- **当** 在 lazyBodies 模式下构建同一输入
- **那么** 其类型引用部分必须与 normal 模式保持一致的基础类型表示

### 需求:By-Stub LazyBodies 基础设施必须稳定识别 `.cj` 文件
By-stub lazyBodies 测试链路必须保证测试输入文件被识别为正确语言文件类型，并可构建可用 stub。

#### 场景:physical + stub 可用
- **当** 测试通过临时物理文件加载 `.cj` 输入
- **那么** 文件视图必须是 physical 且根文件 stub 必须可用

#### 场景:禁止 UNKNOWN 文件类型污染结果
- **当** by-stub 测试执行
- **那么** 测试流程必须避免 `File type: UNKNOWN` 路径导致的伪失败或结果失真

### 需求:RawBuilder 测试必须具备全量输入覆盖护栏
测试生成或执行层必须提供对 testData 输入的全量覆盖校验，确保新增 `.cj` 用例不会被静默遗漏。

#### 场景:新增 testData 自动受检
- **当** 在 `testData/rawBuilder` 下新增 `.cj` 文件但未补齐生成测试
- **那么** 测试必须失败并提示存在未覆盖输入

## 修改需求

### 需求:CjBasicType 转换应覆盖基础类型
当前 raw-cfir 实现要求中关于类型转换的描述必须扩展为：在 Raw CFIR 阶段，`CjBasicType` 与 `CjUserType` 均必须生成可用未解析类型引用，禁止将基础类型作为兜底错误输出。

#### 场景:topLevelFunction 输出正确类型
- **当** `topLevelFunction.cj` 包含 `Int64` 参数与返回值
- **那么** 期望输出必须展示稳定的基础类型引用文本，而不是 `Unsupported type element`

### 需求:Golden File 更新策略必须与文档一致
现有规范中关于更新 golden 的说明必须收敛为“显式开关控制”，并与 README/测试实现保持一致。

#### 场景:文档与实现一致
- **当** 开发者按文档执行默认测试与更新测试
- **那么** 行为必须分别对应“只比较不改写”与“允许改写”两种模式

## 移除需求
