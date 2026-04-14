## 新增需求

### 需求:ConstEval 组全部诊断必须被覆盖
系统必须覆盖 `ConstEval` 组的全部诊断定义：`LITERAL_NUMERIC_OVERFLOW`、`CONST_EVAL_DIVIDE_BY_ZERO`、`CONST_EVAL_ARITHMETIC_OVERFLOW`、`CONST_EVAL_NEGATIVE_SHIFT_COUNT`、`CONST_EVAL_SHIFT_COUNT_OVERFLOW`。

#### 场景:字面量或常量表达式溢出
- **当** 数值字面量超出范围，或常量求值中的算术结果溢出
- **那么** 必须分别报告 `LITERAL_NUMERIC_OVERFLOW` 或 `CONST_EVAL_ARITHMETIC_OVERFLOW`

#### 场景:常量求值发生非法运算
- **当** 常量求值发生除零、负 shift 或 shift 计数超界
- **那么** 必须分别报告 `CONST_EVAL_DIVIDE_BY_ZERO`、`CONST_EVAL_NEGATIVE_SHIFT_COUNT`、`CONST_EVAL_SHIFT_COUNT_OVERFLOW`
