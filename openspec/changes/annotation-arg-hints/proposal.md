# Proposal: 注解实参名提示

## 状态

**已完结**：共享层、LSP、IDE 三侧均已接线（实现记录见 `add-inlay-hints` design D56/D57）。
与立项方案的两处偏差：解析退化为「符号优先 + 同文件按名查」（宏 callee 在 Analysis API
里解析不出来），因此跨文件注解暂无提示。下面是立项时的方案，保留原样以便对照。

## 为什么

`@Ann(1, 2)` 这类注解调用里，每个实参对应哪个形参只能靠数位置或者翻注解声明。
Kotlin 的注解实参名提示是常规体验，仓颉侧目前完全没有。

`add-inlay-hints` 变更在 P1 试过一版实现后回退，原因是只在 `CjAnnotation` 上取候选，
而声明前缀位置的 `@X` 根本不会产出 `CjAnnotation`——入口选错了，不是提示层做不了。

## 关键事实（已用探针实测，见 add-inlay-hints design D56）

`@Ann(1, 2)` + `func demo()` 的实际 PSI：

```
FILE
  MACRO_EXPRESSION `@Ann(1, 2)`
    AT `@`
    REFERENCE_EXPRESSION `Ann`
    MACRO_INPUT `(1, 2)`
      LPAR / QUOTE_TOKENS(INTEGER_LITERAL COMMA INTEGER_LITERAL) / RPAR
  FUNC `func demo() {}`        ← 声明是兄弟节点，没有被宏吞掉
```

两条结论决定了方案：

1. **不必改解析器**：`@X` 与宏声明的语法歧义在 PSI 层不影响可用性，声明始终是独立节点。
2. **实参被 token 化**：`MACRO_INPUT` 里只有原始 token，没有 `CjExpression`。
   这是唯一的技术障碍，必须重建表达式。

## 方案

1. **实参重建**（新增，共享层内部工具）
   取 `CjMacroInput.text`（形如 `(1, 2)`），交给
   `CjPsiFactory.createCallArguments(text)` 解析成 `CjValueArgumentList`，
   再逐个 `getArgumentExpression()`。
   偏移映射：`源偏移 = input.startOffset + 1 + (arg.textRange.startOffset - 实参列表.startOffset - 1)`。
   非括号形态（`@Ann { ... }` 或 `@Ann` 后直接跟声明）返回空。
2. **注解识别**（唯一需要解析的一步）
   候选 = `CjMacroExpression` 且 `input` 为括号形态。
   解析 `referenceExpression` 的引用；若目标不是 `CjTypeStatement`（宏声明、无法解析等）则跳过。
   形参名取该类型的**主构造函数** `valueParameters`，顺序即位置实参的对应关系。
3. **产出**
   对每个**非命名**实参（`CjValueArgument.isNamed()` 为 false），在实参起点插
   `"<形参名>: "`，与 `CjParameterNameHintsCollector` 的文本形态一致。
   位置实参多于形参、或类型无法解析时，整条注解跳过（不做部分推测）。

## 影响面

- 共享层：`CjAnnotationArgumentHintsCollector` + 内部重建工具；`CangJieInlayHintOptions`
  增 `showAnnotationArgumentNames`；新 target `ANNOTATION_ARGUMENT_NAME`。
- LSP：随共享收集器自动生效，新增 golden。
- IDE：**需要新建文案与 EP**——`CangJieBundle` 里没有注解提示相关的 key，
  须新增 `inlay.cangjie.annotations.hints` 与其逐项 key（含 zh_CN），再注册 provider。

## 验收

- 候选/重建：纯解析测试覆盖 `(1, 2)`、嵌套括号与字符串里的逗号、命名实参混合、非括号输入。
- LSP：`annotationArgs.cj` golden 逐字符比对（同文件声明注解类）。
- IDE：provider 注册 + `optionIdOf` 映射 + 资源一致性（复用 `add-inlay-hints` 的
  `testProviderResourcesAreConsistent`）。
- 跳过条件必须有测试：宏声明（`macro` 而非注解类）、无法解析的注解名、实参数多于形参数。

## 风险

- `CjPsiFactory.createCallArguments` 依赖 `let x = foo (args)` 这种合成上下文，
  字符串模板、lambda 实参等边缘形态可能重建失败——失败即跳过，不硬猜。
- 注解类可能声明在其它包（stub 未加载），解析失败时该注解无提示；这是可接受的降级。
