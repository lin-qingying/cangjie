import os

# 1) 我的记忆：更新 cjmp-phase01-landed 至 Phase 0–3
p = r'C:/Users/lin17/.claude/projects/D--code-intellij-cangjie/memory/cjmp-phase01-landed.md'
with open(p, encoding='utf-8') as f:
    t = f.read()
old = '计划：`docs/cjmp-implementation-plan-20260923.md`（Phase 0–5）。2026-09-24 实施 Phase 0–2；Phase 0/1 已验证（diagnostics2 套件绿），Phase 2 待编译验证（gradle 队列被另一会话长任务占用）。'
new = '''计划：`docs/cjmp-implementation-plan-20260923.md`（Phase 0–5，§10 有逐批验收记录）。2026-09-24 实施 Phase 0–3（核心批次）；全部经
「四模块编译 + `*CfirAnalysisDiagnostics*` 全族切片（1224 用例）」队列验证为绿。

**Phase 3（检查器改造，已验证）**：覆盖面修复（`CfirCommonSpecificChecker` 从仅 CfirClass 扩到 class/struct/interface/enum）；
配对结果一律消费 `CfirCjmpMappingStorage`（11 处 symbolProvider 现查移除，MULTIPLE_COMMON_IMPLEMENTATIONS 迁至 specific 侧第二绑定消费）；
门禁收口 `CjmpGate`（新增非上报型 `isEnabled`，三处入口统一）；`CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS` 按官方
`CheckAbstractClassMembers` 重写（specific∧abstract∧仅 CLASS_DECL）；`NOT_MATCHED` 参数形对齐官方（side, "Kind 'name'", counterpart）；
非穷尽枚举 `COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH` 接线（`CfirEnum.isNonExhaustive` 双路径）；注解多重集一一对应
（`annotationKeys` List + special-handled 排除 14+6 项，官方 CheckCJMPAnnotations.cpp:198-296）；接口成员豁免（MustMatchWithPlatform 第 5 条）；
`COMMON_GENERIC_RENAME_NOT_SUPPORTED` 全链删除（官方无触发点）。

**存储落点（重要架构事实）**：`CfirCjmpMappingStorage` 位于 **cfir-tree** 的 `...cfir.session` 包（先例 CfirLanguageSettingsComponent），
非 cfir/resolve——checkers 模块无 `:cfir:resolve` 依赖，会话组件必须落在双方可见层。'''
if old in t:
    t = t.replace(old, new, 1)
    with open(p, 'w', encoding='utf-8') as f:
        f.write(t)
    print('my memory updated')
else:
    print('my memory anchor missing')

# 2) 索引 hook 更新
p2 = r'C:/Users/lin17/.claude/projects/D--code-intellij-cangjie/memory/MEMORY.md'
with open(p2, encoding='utf-8') as f:
    t2 = f.read()
old2 = '- [cjmp-phase01-landed](cjmp-phase01-landed.md) — cjmp Phase 0–2 落地（硬关键字/装填/parse 族 checker/配对引擎）+ cjc 1.1.3 cjmp 探针流程与 gradle 队列用法'
new2 = '- [cjmp-phase01-landed](cjmp-phase01-landed.md) — cjmp Phase 0–3 落地（硬关键字/装填/parse 族 checker/配对引擎/检查器改造）+ cjc 1.1.3 探针流程与 gradle 队列用法'
if old2 in t2:
    t2 = t2.replace(old2, new2, 1)
    with open(p2, 'w', encoding='utf-8') as f:
        f.write(t2)
    print('MEMORY.md hook updated')
else:
    print('MEMORY.md hook unchanged')

# 3) workbuddy 追加
p3 = 'D:/code/intellij/cangjie/.workbuddy/memory/2026-09-24.md'
entry = '''
## cjmp Phase 3 核心批次（同日后续）

- 覆盖面/存储消费/门禁/官方判据修正/参数形/注解多重集/接口豁免/死诊断删除，全部经
  「`:cfir:checkers:compileKotlin :cfir:resolve:compileKotlin :analysis:low-level-api-cfir:compileKotlin` +
  `:cfir:analysis-tests:test --tests "*CfirAnalysisDiagnostics*"`（1224 用例）」队列验证为绿（QP3D–QP3I）。
- 关键结构迁移：`CfirCjmpMappingStorage` 迁至 `cfir/cfir-tree/.../cfir/session/`（cfir:checkers 无 resolve 依赖；会话组件须落双方可见层，
  先例 `CfirLanguageSettingsComponent`）。7 个引用点导入随迁更新。
- fixture：7 个 `diagnostics2/common-specific` 占位按新语义补 NOT_MATCHED 包裹；2 个 `coverage/declaration-status` 补 parse 族期望。
- 全量 `:cfir:analysis-tests:test` 曾以 1.5h 限时尝试（仅完成 123 类）后主动停止——切片验收口径取代；记录于计划文档 §10。
- 剩余登记：泛型宽严（D6）、enum ctor 配对（需载体）、extend 键配对细化、COMMON_WITH_DEFAULT 派生（D8）、sema explicitly-abstract 死条目清理；
  Phase 4（driver/序列化）与 Phase 5（fixture 全量）未开始。
'''
if os.path.exists(p3):
    with open(p3, encoding='utf-8') as f:
        t3 = f.read()
    if 'cjmp Phase 3 核心批次' not in t3:
        with open(p3, 'a', encoding='utf-8') as f:
            f.write(entry)
        print('workbuddy appended')
    else:
        print('workbuddy already has entry')
else:
    with open(p3, 'w', encoding='utf-8') as f:
        f.write('# 2026-09-24\n' + entry)
    print('workbuddy created')
