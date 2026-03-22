## 1. 概念盘点与边界冻结

- [ ] 1.1 盘点 `resolution.common` 中所有 Kotlin 专属概念与 K1-only 路径，并按“删除 / 重写 / 保留”三类建立清单
- [ ] 1.2 对照 `common` 的 `TypeSystemContext*.kt` 与官方 C++ 参考，确认每类概念的仓颉目标语义边界
- [ ] 1.3 标记必须保留的内部推断机制（如 captured/stub/constraint-store）与必须删除的 Kotlin 语言语义

## 2. 公共契约与特性开关收敛

- [ ] 2.1 清理 `resolution.common` 中的 `@K1Deprecation`、Legacy 实现、K1 依赖注入入口和仅用于 K1 兼容的分支
- [ ] 2.2 重写或删除 `resolution.common` 中依赖 Kotlin 专属 `LanguageFeature` 的逻辑，使其只保留仓颉实际需要的门控
- [ ] 2.3 如有必要，调整 `common/src/org/cangnova/cangjie/LanguageVersionSettings.kt` 以反映最终保留的仓颉特性集合

## 3. 类型近似与公共父类型计算清理

- [ ] 3.1 重写 `types/TypeApproximatorConfiguration.kt`，删除 FlexibleType、raw/dynamic、K1/K2 兼容导向的配置项
- [ ] 3.2 重写 `types/AbstractTypeApproximator.kt`，去除 Kotlin 风格 nullability、variance、star projection、DefinitelyNotNull 和 FlexibleType 分支
- [ ] 3.3 重写 `resolve/calls/NewCommonSuperTypeCalculator.kt`，使公共父类型计算仅依赖仓颉存在的类型概念与 `common` 契约

## 4. 约束系统与类型检查辅助清理

- [ ] 4.1 重写 `TypeCheckerStateForConstraintSystem.kt` 与 `ConstraintInjector.kt` 中依赖 Kotlin nullability/projection/flexible type 的逻辑
- [ ] 4.2 清理 `ConstraintIncorporator.kt`、`ResultTypeResolver.kt`、`ConstraintSystemUtilContext.kt` 等文件中的 Kotlin 专属概念与历史兼容入口
- [ ] 4.3 删除或迁移 `LegacyVariableReadinessCalculator.kt`、`VariableFixationFinder.kt`、`NewConstraintSystemImpl.kt` 中仅为 K1/K2 迁移保留的结构

## 5. 慢断言接线

- [ ] 5.1 在 `common/src/org/cangnova/cangjie/type/AbstractTypeChecker.kt` 中增加 `RUN_SLOW_ASSERTIONS` 开关
- [ ] 5.2 将适合保留的内部一致性检查迁移为仓颉语义下的 guarded assertions，禁止改变 subtype/equality 结果
- [ ] 5.3 为测试或调试入口补齐慢断言开启方式，保持默认编译路径不受影响

## 6. 验证与收尾

- [ ] 6.1 对所有修改文件运行 LSP 诊断并修复零错误
- [ ] 6.2 运行 `resolution.common` 与 `common` 的定向编译 / 测试，验证契约收敛后模块可通过
- [ ] 6.3 更新 `README.md` 记录本次 `resolution.common` 类型系统对齐工作的状态、范围与后续遗留问题
