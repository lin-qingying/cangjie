## 新增需求

### 需求:Resolve 组全部诊断必须被覆盖
系统必须覆盖 `Resolve` 组的全部诊断定义：`NO_CONSTRUCTOR`、`ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR`。

#### 场景:普通类型调用但不存在构造器
- **当** 名称已成功解析到类型，但目标类型不存在可调用构造器
- **那么** 必须报告 `NO_CONSTRUCTOR`

#### 场景:直接把枚举类型当作构造器调用
- **当** 代码直接以 `EnumType(...)` 形式调用枚举类型而不是具体枚举构造器
- **那么** 必须报告 `ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR`

### 需求:Resolve 诊断不得退化为通用未解析错误
系统必须在 resolve 阶段给出专用诊断，不得把已知的构造器语义错误退化成更模糊的错误。

#### 场景:可判定为构造器语义错误
- **当** resolve 已经能够区分“无构造器”和“枚举类型误用为构造器”
- **那么** 必须优先报告对应的 `Resolve` 诊断
- **并且** 不得退化为 `UNRESOLVED_REFERENCE` 或其他通用错误
