## 新增需求

### 需求:Match 组全部诊断必须被覆盖
系统必须覆盖 `Match` 组的全部诊断定义：`NON_EXHAUSTIVE_MATCH`、`TUPLE_PATTERN_NOT_MATCH`、`PATTERN_NOT_MATCH`、`ENUM_PATTERN_PARAM_SIZE_ERROR`、`NOT_OVERLOAD_IN_MATCH`、`MATCH_CASE_HAS_NO_TYPE`。

#### 场景:match 不穷尽
- **当** match 表达式未覆盖所有必要分支
- **那么** 必须报告 `NON_EXHAUSTIVE_MATCH`

#### 场景:模式与被匹配值形状不兼容
- **当** tuple pattern、普通 pattern 或 enum pattern 参数个数与被匹配值不兼容
- **那么** 必须分别报告 `TUPLE_PATTERN_NOT_MATCH`、`PATTERN_NOT_MATCH`、`ENUM_PATTERN_PARAM_SIZE_ERROR`

#### 场景:match 中的模式或 case 类型非法
- **当** match 中使用了不允许的 overload 语义，或 case 结果类型无法确定
- **那么** 必须分别报告 `NOT_OVERLOAD_IN_MATCH`、`MATCH_CASE_HAS_NO_TYPE`
