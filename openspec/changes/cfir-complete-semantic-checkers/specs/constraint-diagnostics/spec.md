## 新增需求

### 需求:Constraint 组全部诊断必须被覆盖
系统必须覆盖 `Constraint` 组的全部诊断定义：`NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER`、`ONLY_ONE_CLASS_BOUND_ALLOWED`、`REPEATED_BOUND`、`CONFLICTING_UPPER_BOUNDS`、`CANNOT_INFER_PARAMETER_TYPE`、`NEW_INFERENCE_ERROR`、`TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR`、`BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION`、`INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION`、`INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION`。

#### 场景:约束声明本身非法
- **当** 约束引用的名字不是类型参数、class bound 超过一个、重复声明 bound，或 upper bounds 彼此冲突
- **那么** 必须分别报告对应的 `Constraint` 诊断

#### 场景:推断无法收敛
- **当** 约束求解无法推断参数类型、触发新推断错误、只允许 input types 却被违反，或 builder inference 多 lambda 受限
- **那么** 必须分别报告对应的 `Constraint` 诊断

#### 场景:推断结果落入空交或可能空交
- **当** 推断出的类型变量落入 empty intersection 或 possible empty intersection
- **那么** 必须分别报告 `INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION` 或 `INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION`

### 需求:Constraint 诊断必须由约束求解负责
系统必须在约束求解阶段产出 `Constraint` 诊断，不得由 checker 对推断结果进行猜测式补报。

#### 场景:约束信息仅在推断期可获得
- **当** 诊断依赖完整的 constraint system 与 type variable 状态
- **那么** 必须在 resolve / constraint 阶段发射该诊断
- **并且** 不得下沉到 checker
