## 新增需求

### 需求:Redeclaration 组全部诊断必须被覆盖
系统必须覆盖 `Redeclaration` 组的全部诊断定义：`CONFLICTING_OVERLOADS`、`REDECLARATION`、`CLASSIFIER_REDECLARATION`。

#### 场景:可调用声明发生冲突重载
- **当** 同一作用域中的多个可调用声明签名冲突且不能合法共存
- **那么** 必须报告 `CONFLICTING_OVERLOADS`

#### 场景:普通声明重定义
- **当** 同一作用域中的命名声明重复定义
- **那么** 必须报告 `REDECLARATION`

#### 场景:分类器重定义
- **当** class、interface、struct、enum、typealias 等分类器声明重复定义
- **那么** 必须报告 `CLASSIFIER_REDECLARATION`
