import os

# 1) 我的记忆索引 hook 更新
p = r'C:/Users/lin17/.claude/projects/D--code-intellij-cangjie/memory/MEMORY.md'
with open(p, encoding='utf-8') as f:
    t = f.read()
old = '- [cjmp-phase01-landed](cjmp-phase01-landed.md) — cjmp Phase 0+1 落地（硬关键字/装填/parse 族 checker）+ cjc 1.1.3 cjmp 探针流程'
new = '- [cjmp-phase01-landed](cjmp-phase01-landed.md) — cjmp Phase 0–2 落地（硬关键字/装填/parse 族 checker/配对引擎）+ cjc 1.1.3 cjmp 探针流程与 gradle 队列用法'
if old in t:
    t = t.replace(old, new, 1)
    with open(p, 'w', encoding='utf-8') as f:
        f.write(t)
    print('MEMORY.md hook updated')
else:
    print('MEMORY.md hook already updated or different')

# 2) 仓库 workbuddy 记忆追加（2026-09-24）
d = 'D:/code/intellij/cangjie/.workbuddy/memory'
os.makedirs(d, exist_ok=True)
p = d + '/2026-09-24.md'
entry = '''
## cjmp（common/specific）Phase 0–2 落地（本会话）

- 计划：`docs/cjmp-implementation-plan-20260923.md`（已追加 §10 实施进度）。
- Phase 0（psi 硬关键字 + 14 条 parse 诊断 + CjmpGate）与 Phase 1（status 装填双路径）**已验证**：
  `:cfir:analysis-tests:test --tests "*CfirAnalysisDiagnostics2*"` 全绿（含改写后的 langver105 版本门 fixture 与 2 个新 parse 规则 fixture）。
  `:psi:test` 74/1（唯一失败 `ForeignAndAnnotationParsingTest.platformAnnotationSurfaceIsRetainedWithoutBuiltinKindFacade`
  归因并行会话的注解 builtin 重构，与本工作无关）。
- Phase 2（配对引擎）代码已落盘、待编译验证：见记忆 `cjmp-phase01-landed`（我的记忆目录）中的文件清单。
- 官方 1.1.3 探针产物：`.workbuddy/tmp/cjmp_probe113/`（run_probes*.sh + probe_results*.txt）；本次新增实测结论：
  12 条 parse 诊断中 3 条无触发点（specific 参数默认值合法、隐式类型 var 合法、泛型走 @Frozen 族诊断）；
  file-part 两条锚在 package 行 1:1；`abstract func` 在非 CJMP 抽象类双报（parse 变体 + 通用 illegal-modifier）。
- 环境教训：**本会话发现 gradle 队列存在另一会话的长任务占用**（`:cfir:analysis-tests:test --continue` 单次运行超 2 小时未结束），
  多会话并行时排队等待是常态；`--show-queue` 看占用者，`--kill-current` 仅在确认是自家僵尸进程时使用。
- 探针环境坑：specific 编译必须 `--common-part-cjo=<path>`（`=` 形式）+ 位置参数给 common 的 `<pkg>.chir`；
  未设 `CANGJIE_HOME` 时 cjc 1.1.3 会误用 1.0.5 的 LLVM 后端（出现 "Reflection format: modifier type is invalid" 崩溃）。
'''
if os.path.exists(p):
    with open(p, encoding='utf-8') as f:
        t = f.read()
    if 'cjmp（common/specific）Phase 0–2 落地' in t:
        print('workbuddy memory already has entry')
    else:
        with open(p, 'a', encoding='utf-8') as f:
            f.write(entry)
        print('workbuddy memory appended')
else:
    with open(p, 'w', encoding='utf-8') as f:
        f.write('# 2026-09-24\n' + entry)
    print('workbuddy memory created')
