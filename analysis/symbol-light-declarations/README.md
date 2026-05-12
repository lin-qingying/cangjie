# analysis/symbol-light-declarations/ — Symbol-based Light Declarations

`:analysis:light-declarations` 的 Symbol-based 变体。把 Analysis API 的 Symbol 适配为 light declaration，供需要"通过 symbol 拿到轻量声明视图"的 IDE 场景使用。

对齐 Kotlin `analysis/symbol-light-classes`。

## 关键包

`org.cangnova.cangjie.analysis.light.declarations.*` — Symbol → light declaration 适配。

## 设计要点

- 不重复 `:analysis:light-declarations` 的模型，复用其类型
- 入口是 Analysis API Symbol 抽象（`KaSymbol` 等）
- 服务于 navigation / refactor / refactor preview 等需要轻量视图的场景

## 依赖

- `:analysis:light-declarations`
- `:analysis:analysis-api`、`:analysis:analysis-api-platform-interface`、`:analysis:analysis-api-impl-base`

## 命令

```bash
./gradlew :analysis:symbol-light-declarations:assemble
./gradlew :analysis:symbol-light-declarations:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
- `../light-declarations/README.md`
