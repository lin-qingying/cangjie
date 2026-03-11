## 1. 解析阶段处理器与注册

- [x] 1.1 为 IMPORTS/TYPES/STATUS/CHECKERS 定义最小 `CfirResolveProcessor` 实现
- [x] 1.2 在 session 初始化或注册入口中注册阶段处理器
- [x] 1.3 在 `CfirTotalResolveProcessor` 路径下验证阶段推进更新 `resolvePhase`

## 2. 最小 provider 支撑

- [x] 2.1 定义最小 `CfirProvider`/`CfirSymbolProvider` 实现（可空/只读）
- [x] 2.2 将最小 provider 注册到 session 组件体系
- [x] 2.3 为 provider 添加最小单元验证或 smoke test

## 3. 诊断测试接入

- [x] 3.1 实现可复用的 `DiagnosticsHandler`（基于 test-infrastructure）
- [x] 3.2 实现 `CfirResolveFacade` 的测试入口并接入 `TestFacade`
- [x] 3.3 新增 `resolveDiagnostics` 测试用例与 golden 文件
