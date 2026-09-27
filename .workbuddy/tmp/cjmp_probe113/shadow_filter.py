p = 'D:/code/intellij/cangjie/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt'
with open(p, 'rb') as f:
    t = f.read().decode('utf-8')


def replace_once(text, old, new, what):
    if text.count(old) == 1:
        return text.replace(old, new, 1)
    nl = '\r\n' if '\r\n' in text else '\n'
    old2, new2 = old.replace('\n', nl), new.replace('\n', nl)
    c = text.count(old2)
    assert c == 1, f"{what}: anchor count {c} (lf={text.count(old)}, crlf={text.count(old2)})"
    return text.replace(old2, new2, 1)


if 'reduceCjmpShadowedCommonCandidates' in t:
    print('already patched')
    raise SystemExit(0)

imp_anchor = 'import org.cangnova.cangjie.cfir.declarations.*'
if 'import org.cangnova.cangjie.LanguageFeature' not in t:
    t = replace_once(t, imp_anchor, imp_anchor + '\nimport org.cangnova.cangjie.LanguageFeature\nimport org.cangnova.cangjie.cfir.resolve.cjmp.cjmpMappingStorage', 'imports')

if 'import org.cangnova.cangjie.cfir.session.languageVersionSettings' not in t:
    anchor2 = 'import org.cangnova.cangjie.cfir.session.CfirSession'
    t = replace_once(t, anchor2, anchor2 + '\nimport org.cangnova.cangjie.cfir.session.languageVersionSettings', 'session import')

call_anchor = '        reducedCandidates = reduceFunctionValueCandidatesByExpectedType(info, reducedCandidates)'
t = replace_once(t, call_anchor, call_anchor + '\n        reducedCandidates = reduceCjmpShadowedCommonCandidates(reducedCandidates)', 'call site')

fn_anchor = '''    private fun collectCandidates(
        info: CallInfo,
        resolutionContext: ResolutionContext,
        collector: CfirCandidateCollector? = null,
    ): ResolutionResult {'''
new_fn = '''    /**
     * CJMP 消费侧遮蔽（D1 的 `FilterOutCommonCandidatesIfSpecificExist` 对位）。
     *
     * specific 模式下，若候选指向的 common 声明已在当前模块完成配对（存储中存在 specific 绑定），
     * 该 common 候选从解析候选中剔除——specific 声明遮蔽依赖模块的 common 同名声明。
     *
     * 门禁纪律（§8.5.5）：版本门关闭或存储为空时不激活，1.0.x 行为零变化。
     */
    private fun reduceCjmpShadowedCommonCandidates(candidates: Set<Candidate>): Set<Candidate> {
        if (candidates.isEmpty()) return candidates
        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.CommonSpecificDeclarations)) return candidates
        val storage = session.cjmpMappingStorage
        if (storage.isEmpty) return candidates
        return candidates.filterTo(linkedSetOf()) { candidate ->
            val declaration = candidate.symbol.takeIf { it.isBound }?.cfir ?: return@filterTo true
            val isCommon = (declaration as? CfirMemberDeclaration)?.status?.isCommon == true
            if (!isCommon) return@filterTo true
            storage.specificBindingsFor(declaration).isEmpty()
        }
    }

''' + fn_anchor
t = replace_once(t, fn_anchor, new_fn, 'function insert')

with open(p, 'wb') as f:
    f.write(t.encode('utf-8'))
print('cjmp shadow filter patched')
