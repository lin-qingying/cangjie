## 新增需求

## 修改需求

### 需求:Raw CFIR 类型转换必须覆盖 VArray 类型引用
Raw CFIR 构建阶段在处理仓颉类型引用时，必须正确表示 `VArray<T, $N>`，并保留元素类型与编译期尺寸字面量信息。系统禁止将 `CjVArrayType` 统一降级为 `Unsupported type element` 错误类型，也禁止把尺寸参数伪装为普通类型实参。

#### 场景:函数签名中的 VArray 类型
- **当** 函数参数、返回值或属性类型声明为 `VArray<T, $N>`
- **那么** Raw CFIR 输出必须展示对应的 `VArray` 类型引用，且保留元素类型与 `$N` 信息

#### 场景:typealias 中的 VArray 类型
- **当** `typealias` 的展开类型为 `VArray<T, $N>`
- **那么** Raw CFIR 输出必须保留 `VArray` 结构，而不是退化为错误类型引用或普通用户类型引用

#### 场景:VArray 的元素类型仍为复合类型
- **当** `VArray` 的元素类型本身是用户类型、元组类型或函数类型
- **那么** Raw CFIR 输出必须继续保留其嵌套类型引用结构

## 移除需求
