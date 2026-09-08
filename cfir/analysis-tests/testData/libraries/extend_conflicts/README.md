# 导入扩展的真实 CJO 测试数据

这些 CJO 由官方 cjc 1.0.5（Windows x86_64）编译，测试只消费前端 common-part 元数据。相邻 `.cj` 文件是对应库源码；正式 LLT 用 `IMPORT_PATH` 加载 CJO，不把这些库源码加入消费模块。

| 目录 | 编译顺序 | 验证内容 |
| --- | --- | --- |
| shadow | A.cj → B.cj、C.cj | 多个库扩展的同名显式成员冲突 |
| nominal | base.cj → a.cj、b.cj | 普通类扩展的默认属性及 DEFAULT 元数据 |
| primitive | a.cj、b.cj | 导入同包普通类不会使未导入的扩展接口可见 |

在对应目录内，依上述顺序执行 `cjc <source.cj> --import-path . --output-dir . --output-type staticlib -o lib<package>.a` 可重建。测试仅需要 `.cjo`，不需要静态库 `.a`。

源码输入的冲突继续报告在扩展声明。CJO 未携带可用于 IDE 下划线的源码文件时，库冲突报告在消费文件中实际导入该扩展的完整 import item。诊断位置选择不改变候选可见性、目标实例化或冲突判断。

验证该路径的 LLT 为 `../../llt/Extend_import/library_shadow_conflicts.cj`、`library_default_conflicts.cj`、`library_default_visibility.cj`，每项均运行 PSI 和 LightTree 两入口。
