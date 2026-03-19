## 1. 配置与模型对齐

- [x] 1.1 在 Cangjie 配置层引入 `CONTENT_ROOTS` 等价键与内容根模型（含源码/依赖类型与模块元信息）
- [x] 1.2 提供 content roots 的构建辅助方法（测试与 CLI 可复用）
- [x] 1.3 为旧 `CLI_SOURCE_FILE_PATHS` 增加兼容映射与弃用提示输出

## 2. 源文件收集流程

- [x] 2.1 在 CFIR 前端管线中改为从 content roots 收集源文件
- [x] 2.2 实现与 Kotlin 等价的 source roots 去重、过滤与顺序稳定性
- [x] 2.3 支持平台源/公共源拆分与模块分组
- [x] 2.4 预留/接入扩展点以允许额外源文件加入

## 3. 测试基础设施与默认配置

- [x] 3.1 在测试配置提供者中写入 content roots（替代 `CLI_SOURCE_FILE_PATHS`）
- [x] 3.2 覆盖 LightTree 与 PSI 两种解析模式的输入一致性
- [ ] 3.3 更新诊断/结构化测试基类，确保输入来源统一

## 4. 迁移与文档

- [x] 4.1 更新开发文档与测试约定说明 content roots 输入
- [x] 4.2 标注 `CLI_SOURCE_FILE_PATHS` 弃用周期与迁移步骤

## 5. 验证

- [ ] 5.1 运行至少一个 const-eval 诊断测试用例，确认 CHECKERS 有输入
- [x] 5.2 增补回归测试，覆盖空输入与重复根路径的诊断
