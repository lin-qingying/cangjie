# cfir/entrypoint/ — CFIR 前端入口与编排

把基础设施（PSI、cfir-tree、cones）、构建链（raw-cfir）、解析链（resolve、checkers）、序列化（cfir-serialization）拼装成可调用的 CFIR 前端 Session 与 Pipeline。

对齐 Kotlin K2 `compiler/fir/entrypoint`。

## 关键包

| 包 | 职责 |
|---|---|
| `cfir.entrypoint.session` | `CfirSession` 工厂（注册 components、registrar、resolver、providers） |
| `cfir.entrypoint.configuration` | Session / Pipeline 配置 |
| `cfir.entrypoint.checkers` | 默认 checkers 装配（`CheckersContainers`） |
| `cfir.pipeline` | 阶段串联（CFIR_BUILD → CFIR_RESOLVE → CHECKERS） |
| `cfir.extensions` | 前端扩展点 |
| `cfir.deserialization` | 反序列化接入（与 `:cfir:cfir-serialization` 协作） |

## 调用方

- `:compiler:frontend` — 编译器前端整体管线
- `:analysis:analysis-api-cfir` — IDE 分析 API 的 CFIR 后端
- `:tests:test-infrastructure` 的 `CfirFrontendFacade` — 测试入口

## 依赖

- `:cfir:resolve`、`:cfir:checkers`、`:cfir:cfir-serialization`
- `:cfir:raw-cfir:psi2cfir`、`:cfir:raw-cfir:light-tree2cfir`
- `:cfir:cfir-tree`、`:cfir:cfir-cones`、`:cfir:cfir-common`、`:cfir:semantics`、`:cfir:providers`
- `:common`、`:common:diagnostics`、`:util`、`:compiler:config`

## 命令

```bash
./gradlew :cfir:entrypoint:assemble
./gradlew :cfir:entrypoint:test
```

## 相关文档

- `../../cjfir-compiler-stages.md` — 阶段编排设计
- `../../docs/current-module-organization.md` — entrypoint 在分层中的位置
