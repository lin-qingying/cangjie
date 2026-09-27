p = 'D:/code/intellij/cangjie/analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirLazyPhaseResolverByPhase.kt'
with open(p, 'rb') as f:
    t = f.read().decode('utf-8')

if 'CJMP_MATCHING' in t:
    print('already registered')
else:
    old = '        this[CfirResolvePhase.BODY_RESOLVE] = LLCfirBodyLazyResolver'
    new = '''        this[CfirResolvePhase.CJMP_MATCHING] = LLCfirCjmpMatchingLazyResolver
        this[CfirResolvePhase.BODY_RESOLVE] = LLCfirBodyLazyResolver'''
    assert t.count(old) == 1, f"anchor {t.count(old)}"
    t = t.replace(old, new, 1)
    with open(p, 'wb') as f:
        f.write(t.encode('utf-8'))
    print('registered CJMP_MATCHING lazy resolver')
