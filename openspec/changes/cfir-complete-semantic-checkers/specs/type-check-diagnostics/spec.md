## 新增需求

### 需求:TypeCheck 组全部诊断必须被覆盖
系统必须覆盖 `TypeCheck` 组的全部诊断定义：`TYPE_MISMATCH`、`PATTERN_INITIALIZER_TYPE_MISMATCH`、`RETURN_TYPE_MISMATCH`、`ARGUMENT_TYPE_MISMATCH`、`ASSIGNMENT_TYPE_MISMATCH`、`VARRAY_SIZE_MISMATCH`、`GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT`、`INVISIBLE_MEMBER`、`INVISIBLE_REFERENCE`、`OVERRIDING_RETURN_TYPE_MISMATCH`、`CANNOT_OVERRIDE_INVISIBLE_MEMBER`、`CLASS_NOT_OPEN_FOR_INHERITANCE`、`ABSTRACT_MEMBER_NOT_IMPLEMENTED`。

#### 场景:值类型与期望类型不一致
- **当** 普通表达式、pattern initializer、返回值、实参、赋值或 VArray 长度约束不满足期望类型
- **那么** 必须分别报告对应的 `TypeCheck` 诊断

#### 场景:类型使用形态非法
- **当** 泛型类型缺少必要类型实参，或访问到了不可见成员 / 不可见引用
- **那么** 必须分别报告 `GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT`、`INVISIBLE_MEMBER`、`INVISIBLE_REFERENCE`

#### 场景:继承与实现语义违反类型规则
- **当** override 返回类型不匹配、试图 override 不可见成员、继承了不可继承的类，或抽象成员未实现
- **那么** 必须分别报告对应的 `TypeCheck` 诊断

### 需求:TypeCheck 诊断必须优先在类型检查主流程产出
系统必须在主类型检查流程中发射 `TypeCheck` 诊断，避免 checker 重复或延后报告。

#### 场景:主类型检查已能确定错误
- **当** 类型检查主流程已经拥有完整的实际类型与期望类型信息
- **那么** 必须直接在该流程中报告对应诊断
- **并且** 不得依赖 checker 进行重复修补
