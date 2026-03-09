## 新增需求

## 修改需求

### 需求:RawBuilder 必须覆盖可恢复的缺失表达式场景
Raw CFIR 构建阶段对可恢复的缺失表达式 PSI 节点必须具备稳定回归测试。系统必须通过现有 rawBuilder golden file 机制为仓颉 parser 可恢复并能进入 `PsiRawCfirBuilder` 的缺失表达式形态建立断言；normal rawBuilder 路径必须比较 `.txt` 基线，lazyBodies 路径必须比较 `.lazyBodies.txt` 基线。系统禁止仅依赖合法声明样例来间接覆盖这些恢复分支。

#### 场景:缺失右操作数仍可进入 builder
- **当** 在 rawBuilder testData 中新增一个二元表达式缺失右操作数、但 PSI 仍能恢复为 `CjBinaryExpression` 的仓颉源文件
- **那么** rawBuilder golden 必须断言对应的稳定恢复输出（如 `ERROR_EXPR(...)` 或等价占位），并且测试执行不得崩溃

#### 场景:控制流或一元表达式缺失必需子表达式
- **当** `if`、`while`、`throw`、空括号表达式等场景缺失必需子表达式，但仍进入 `PsiRawCfirBuilder`
- **那么** rawBuilder golden 必须锁定 builder 当前约定的恢复输出（错误表达式、空 block 或空结果），防止后续修改悄悄改变恢复行为

### 需求:RawBuilder 表达式测试必须被生成套件完整发现
RawBuilder 的表达式测试目录必须被生成测试套件完整发现。系统必须保证新增到 `cfir/raw-cfir/psi2cfir/testData/rawBuilder/expressions` 的 `.cj` 文件能够进入主 rawBuilder 与 lazyBodies（by-ast / by-stub）测试路径，并拥有对应的 golden 基线文件。

#### 场景:向 expressions 目录新增用例
- **当** 在 `cfir/raw-cfir/psi2cfir/testData/rawBuilder/expressions` 下新增 `.cj` 测试文件
- **那么** 生成测试代码后必须出现对应的 rawBuilder、lazyBodies by-ast 和 lazyBodies by-stub 测试方法，其中 normal rawBuilder 必须比较 `.txt` 基线，而两个 lazyBodies 套件必须比较共享的 `.lazyBodies.txt` 基线

#### 场景:表达式目录与声明目录并存
- **当** rawBuilder 同时包含 `declarations/` 与 `expressions/` 两类测试目录
- **那么** all-files-present 等效校验必须覆盖两类目录，禁止出现主 rawBuilder suite 已覆盖但 lazyBodies suite 漏测新增目录的情况

## 移除需求
