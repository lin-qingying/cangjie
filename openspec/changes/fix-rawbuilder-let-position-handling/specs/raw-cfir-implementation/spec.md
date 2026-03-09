## 新增需求

### 需求:RawBuilder 必须识别类体中的简单成员字段声明
Raw CFIR 构建阶段必须保留现有具名单一变量的 `CfirVariable` 建模，并新增独立的 `CfirPatternVariable : CfirCallableDeclaration`。其中，`CfirVariable` 继续表示具名单一变量；`CfirPatternVariable` 表示拥有完整 `CfirPattern` 的模式变量声明，而不是单一绑定名。对于 class/interface/struct/enum 体内以 `CjFieldVariable` 暴露的简单命名成员字段，系统必须继续 lowering 为 `CfirVariable`，并保留名称、可变性、显式类型与初始化器信息。系统禁止将这类成员字段统一退化为 `<error-declaration>` 占位，也禁止将其直接混同为 `Property/PropDecl`。

#### 场景:classWithMembers 中的 let 与 var 成员字段
- **当** rawBuilder 构建 `class WithMembers { var x: Int64 = 0; let name: String = "hello" ... }`
- **那么** 输出必须包含 `CfirVariable` 对应的具名成员字段声明，并正确区分 `var x: Int64` 与 `let name: String`

#### 场景:成员字段出现在不同成员顺序中
- **当** simple named member fields 位于函数前、函数后、构造器前后或与其他成员交错出现
- **那么** rawBuilder 与 lazyBodies 输出都必须稳定反映这些字段，而不得因为位置变化再次退化为 `<error-declaration>`

#### 场景:非成员 pattern variable 不被伪装为成员字段支持
- **当** 输入声明在文件级或局部作用域中以 `CjPatternVariable` 表示，且可能包含 tuple、enum 或 wildcard pattern
- **那么** 系统不得将其伪装为 `CfirVariable` 或 `Property`；即使完整 lowering 暂未实现，也必须为其保留 `CfirPatternVariable(pattern)` 的独立建模方向

#### 场景:pattern variable 的绑定列表来自 pattern 派生
- **当** 一个 `CfirPatternVariable` 持有 tuple、enum、type 或 wildcard 等复合模式
- **那么** 系统必须保留完整 `pattern` 结构，并通过派生查询获得 0..N 个 binding / declaration sites，而不是在 raw CFIR 中重复存储展开后的绑定变量节点

## 修改需求

## 移除需求
