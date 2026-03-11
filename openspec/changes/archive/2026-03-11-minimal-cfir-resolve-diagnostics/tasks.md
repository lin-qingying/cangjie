## 1. 解析入口与诊断管线骨架

- [x] 1.1 建立最小 CfirResolveFacade 与测试调用入口（对接现有 resolve processor）
- [x] 1.2 接入空诊断收集链路（CfirDiagnosticCollector/Reporter），确保可执行

## 2. 测试入口与基线

- [x] 2.1 新增最小 DiagnosticsHandler（基于 test-infrastructure 的 AnalysisHandler）
- [x] 2.2 添加最小诊断测试用例与期望输出（允许空诊断）
- [x] 2.3 校验测试框架能完成 facade → handler 的完整调用链
