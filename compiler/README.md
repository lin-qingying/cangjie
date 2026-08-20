# compiler/ — 编译器驱动与可选后端

承载编译器**驱动层**（配置、阶段管理、参数、前端协调、插件）与**可选后端**（CHIR、CodeGen）。本目录是聚合命名空间。

## 子模块

### 驱动与配置

| 子模块 | 职责 |
|---|---|
| `config` | `CompilerConfiguration` / Content Roots / 环境模型 |
| `phaser` | 阶段管理框架（`CompilerPhase` / `PhaseSet` / `PhaserState`） |
| `arguments` | 命令行参数定义 |
| `frontend-arguments-generator` | 参数代码生成器 |
| `frontend` | 前端基础设施与编译管线协调（详见 `frontend/README.md`） |
| `plugin` | 编译器插件加载占位（阶段 1 LOAD_PLUGINS） |

### 可选后端

| 子模块 | 职责 |
|---|---|
| `:chir:chir-tree` | CHIR（Cangjie High-level IR）数据模型与 pass 框架（见 [`../chir/README.md`](../chir/README.md)） |
| `codegen` | CHIR → LLVM IR 代码生成后端（详见 `codegen/README.md`） |

## 与管线对应

前端管线核心阶段（阶段 1 ~ 10）以 `:compiler:frontend` 为编排入口，配合 `:psi`、`:cfir:*`、`:macro:*`、`:resolution.common` 等模块完成。可选后端通过 `:chir:cfir2chir` + `:chir:chir-tree` + `:compiler:codegen` 接入 LLVM 后端。

## 相关文档

- `../docs/cjfir-compiler-stages.md` — 完整阶段设计
- `../chir/chir-tree/docs/module-boundary.md` — CHIR 模块边界
- `codegen/docs/cpp-codegen-mapping.md` — CodeGen 官方对照
