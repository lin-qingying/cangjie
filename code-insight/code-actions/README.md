# code-insight/code-actions/ — 快速修复与成员生成

诊断驱动的代码修改入口。quick fixes 与 override/implement 共用同一交互通道（intention / LSP codeAction），因此放在一个模块；成员生成对外仍只通过 `:code-insight:api` 的窄接口暴露。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.ide.codeInsight.fixes` | quick-fix factory 与 registrar：修饰符族、import 族、声明族等诊断 → 修复注册 |
| `org.cangnova.cangjie.ide.core.overrideImplement` | override/implement 动作处理器、`CangJieMemberGenerationService` 实现与 headless 注册 |

## 依赖

- `:code-insight:api`
- `:code-insight:completion` — import 族 quick fix 复用补全链路的导入计划契约与 PSI 应用器 `ImportPlanApplier`
- `:analysis:analysis-api`
- `:analysis:analysis-api-cfir`
- `:psi`
- `:common`

## 命令

```bash
./gradlew :code-insight:code-actions:assemble
./gradlew :code-insight:code-actions:test
```

## 相关文档

- `../README.md` — Code Insight 总览
- `../../docs/module-catalog.md` — 模块清单
- `../../docs/仓颉快速修复框架：IDE LSP 共享执行方案.md` — 快速修复框架设计
- `../../docs/quickfix-gap-analysis.md` — 快速修复缺口分析
