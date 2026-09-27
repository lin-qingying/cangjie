#!/bin/bash
# cjmp Phase 0–2 落地文件暂存脚本（提交前先跑编译验证；本脚本只列文件，不执行 add）
# 注意：cfir/resolve/.../body/CfirCallResolver.kt 含会话前既有改动（他人 hunks），
#       提交时须用 hunk 过滤（参考 .workbuddy/memory/2026-09-23.md 的 filter_hunks.py 流程），
#       不得直接 git add 整个文件。
cat <<'LIST'
# ---- psi（Phase 0 词法/语法）----
psi/src/org/cangnova/cangjie/lexer/CjTokens.java
psi/src/org/cangnova/cangjie/lexer/CangJieLexer.flex
psi/gen/org/cangnova/cangjie/lexer/_CangJieLexer.java
psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt
psi/src/org/cangnova/cangjie/psi/stubs/CangJieStubVersions.kt
psi/test/org/cangnova/cangjie/psi/ModifierParsingTest.kt

# ---- 诊断清单/消息/生成物 ----
cfir/checkers/checkers-component-generator/src/org/cangnova/cangjie/cfir/checkers/generator/diagnostics/CfirDiagnosticsList.kt
cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt
cfir/checkers/gen/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrors.kt
cfir/checkers/gen/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirNonSuppressibleErrorNames.kt

# ---- checkers（门禁/parse 族 checker/修饰符表）----
cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CjmpGate.kt
cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCjmpParseRulesChecker.kt
cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CommonDeclarationCheckers.kt
cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/ModifierCheckerTargets.kt
cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/Compatibility.kt
cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/ModifiersCompatibilityUtils.kt

# ---- raw CFIR 装填（Phase 1）----
cfir/raw-cfir/raw-cfir-common/src/org/cangnova/cangjie/cfir/builder/AbstractRawCfirBuilder.kt
cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt
cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeModifierList.kt

# ---- Phase 2 配对引擎 ----
cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirResolvePhase.kt
cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMappingStorage.kt
cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpResolver.kt
cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatcher.kt
cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatchRunner.kt
cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt
cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirResolveProcessors.kt
cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt    # 混合 hunks，需按 hunk 过滤
cfir/entrypoint/src/org/cangnova/cangjie/cfir/session/ComponentsContainers.kt

# ---- LL 管线入口（G16）----
analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirCjmpMatchingLazyResolver.kt
analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirLazyPhaseResolverByPhase.kt

# ---- fixtures ----
cfir/analysis-tests/testData/diagnostics2/common-specific/*.cj
cfir/analysis-tests/testData/diagnostics/coverage/accessibility/cstAccessibleCommonParent.cj
cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisDiagnostics2*Generated.kt

# ---- 文档 ----
docs/cjmp-implementation-plan-20260923.md
LIST
