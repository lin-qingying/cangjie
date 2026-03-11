## 1. 迁移与基础支架

- [x] 1.1 盘点并标记 `CfirMinimalResolveProcessors`、`CfirMinimalResolvePipelineTest`、`MinimalResolveDiagnosticsPipelineTest` 的 legacy 状态
- [x] 1.2 在 `org.cangjie.cfir.resolve` 建立正式实现目录结构（`processors/services/providers/diagnostics`）
- [x] 1.3 在 `CfirResolveComponentsRegistrar` 中明确“正式路径优先，minimal 仅兼容”
- [x] 1.4 建立 phase 级回滚策略（按阶段可单独回退）

## 2. 官方语义映射（Rule Catalog）

- [x] 2.1 建立规则 ID 与官方实现映射（`external/cangjie_compiler/src/Sema`）
- [x] 2.2 为 `IMPORTS/SUPER_TYPES/TYPES/STATUS/EXTENSIONS/IMPLICIT_TYPES/BODY_RESOLVE/CHECKERS` 补齐规则映射表
- [x] 2.3 统一诊断工厂命名并绑定规则 ID
- [x] 2.4 增加“规则映射完整性”校验测试

## 3. Provider 与 Session 正式化

- [x] 3.1 设计并实现组合式 provider 链（`source/import/builtin/extend`）
- [x] 3.2 默认注册正式 provider 组合，替换空 provider 主路径
- [x] 3.3 将 minimal provider 隔离为兼容/测试专用装配
- [x] 3.4 增加跨包查询、类查询、extend 查询回归测试

## 4. IMPORTS 阶段

- [x] 4.1 实现 import 绑定（包、类型、可调用符号、别名）
- [x] 4.2 实现不存在导入目标诊断
- [x] 4.3 实现导入冲突与别名冲突诊断
- [x] 4.4 增加 total/lazy 在 IMPORTS 阶段一致性测试

## 5. SUPER_TYPES 阶段

- [x] 5.1 实现继承图构建与基础校验（自继承、重复接口、接口继承类、多类父类型）
- [ ] 5.2 实现泛型实例化后重复超接口检测
- [ ] 5.3 实现错误恢复（局部失败不阻断文件后续声明）
- [ ] 5.4 增加 SUPER_TYPES 正负向测试矩阵

## 6. TYPES 阶段

- [ ] 6.1 实现显式类型引用解析（参数、返回、字段、约束）
- [ ] 6.2 补齐仓颉类型形态解析（含 tuple/function type/VArray）
- [ ] 6.3 实现类型解析失败错误占位与恢复策略
- [ ] 6.4 增加 TYPES 阶段语义与诊断测试

## 7. STATUS 阶段

- [ ] 7.1 实现修饰符组合合法性检查
- [ ] 7.2 实现可见性与上下文约束检查
- [ ] 7.3 输出标准化声明状态供后续阶段消费
- [ ] 7.4 增加 STATUS 阶段测试（合法/非法组合）

## 8. EXTENSIONS 阶段

- [ ] 8.1 实现 extend 目标合法性检查（非法 extended type、接口目标限制）
- [ ] 8.2 实现接口实现列表检查（重复、非接口、特化冲突）
- [ ] 8.3 实现跨包扩展规则（含孤儿规则语义）
- [ ] 8.4 增加 EXTENSIONS 全量规则测试

## 9. IMPLICIT_TYPES 阶段

- [ ] 9.1 实现声明边界隐式类型推断（变量/函数返回类型）
- [ ] 9.2 实现互依赖推断与循环推断检测
- [ ] 9.3 推断失败时生成稳定错误类型与诊断
- [ ] 9.4 增加 IMPLICIT_TYPES 阶段测试（含循环场景）

## 10. BODY_RESOLVE 阶段

- [ ] 10.1 实现表达式类型求值与调用绑定
- [ ] 10.2 实现控制流表达式语义（if/match/loop）
- [ ] 10.3 实现仓颉特有表达式语义（含 `spawn`）
- [ ] 10.4 实现局部失败恢复（不中断函数体后续解析）
- [ ] 10.5 增加 BODY_RESOLVE 阶段测试矩阵

## 11. CHECKERS 阶段

- [ ] 11.1 实现 resolve 终态规则检查
- [ ] 11.2 确保 CHECKERS 只消费前序语义结果，不重复完整求值
- [ ] 11.3 稳定化诊断输出顺序与文本
- [ ] 11.4 增加 CHECKERS 阶段回归测试

## 12. Jumping Phase 机制

- [ ] 12.1 固化 jumping phase 白名单并提供可审计定义
- [ ] 12.2 实现 same-phase 限制（仅 `IMPLICIT_TYPES`、`BODY_RESOLVE`）
- [ ] 12.3 实现 jumping 请求前置阶段补齐策略（禁止绕过依赖阶段）
- [ ] 12.4 实现跨声明循环检测与受控退出
- [ ] 12.5 实现 jumping 请求幂等保证（重复请求结果不变）
- [ ] 12.6 增加 total/lazy 对齐测试（到同目标阶段结果一致）
- [ ] 12.7 增加 jumping 失败路径可观测性测试（稳定失败标识）

## 13. 诊断与测试体系收敛

- [ ] 13.1 将 synthetic 诊断测试扩展为真实 `.cj` 输入 + golden
- [ ] 13.2 建立全阶段测试矩阵（每阶段至少 1 个正向 + 1 个负向）
- [ ] 13.3 增加 Analysis API 端到端测试（`resolveTo(..., CHECKERS)`）
- [ ] 13.4 保留 legacy 兼容回归测试并标注退役时间

## 14. 文档与验收

- [ ] 14.1 更新 `compiler-architecture`、`raw-cfir-implementation` 的阶段状态与边界文档
- [ ] 14.2 更新开发说明（resolve 生命周期、jumping 机制、规则映射）
- [ ] 14.3 执行 `:cfir:cfir-tree:test` 与 `:analysis:analysis-api-cfir:test` 并修复回归
- [ ] 14.4 输出最终验收报告（阶段覆盖、语义对齐、测试覆盖、遗留风险）
