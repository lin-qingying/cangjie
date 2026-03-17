## 1. 会话构建入口改造

- [ ] 1.1 梳理现有 CfirSessionConstructionUtils 与 CLI pipeline 调用路径，明确多模块/metadata 所需输入
- [ ] 1.2 设计模块级会话构建流程（shared/library/source）与 moduleData 构建策略
- [ ] 1.3 重写 prepareSessions 以使用注入回调并返回模块级 SessionWithSources

## 2. 元数据与多模块行为落地

- [ ] 2.1 补齐 metadata 模式下的会话构建路径与分支条件
- [ ] 2.2 更新调用方以适配多模块 SessionWithSources 输出
- [ ] 2.3 为多模块/metadata 行为补充验证点或测试计划
