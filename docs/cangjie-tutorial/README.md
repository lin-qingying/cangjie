# 仓颉语言教程站

本目录是一个独立的 VitePress 教程站。VitePress 会把 `docs/` 下按学习路径拆分的 Markdown 页面构建成静态网页。

教程导航由 `docs/.vitepress/config.mts` 维护；语言语义和代码示例以官方仓颉资料与 `cjc` 为准，不以本仓库的 CFIR 实现状态为准。资料入口见[教程资料来源](docs/appendix/sources.md)和[主仓库语言参考](../README.zh-CN.md)。

## 本地使用

```bash
npm install
npm run dev
```

构建静态网页：

```bash
npm run build
```

预览构建结果：

```bash
npm run preview
```

## 内容边界

- 教程面向仓颉语言学习者，以命令行任务本为主线。
- 内容覆盖工具链、入口、变量、表达式、集合、函数、建模、泛型、包、异常、I/O、并发、宏和 FFI。
- 仓颉代码示例统一使用 `cj` 代码块，并由 VitePress 注册的自定义语法高亮。
