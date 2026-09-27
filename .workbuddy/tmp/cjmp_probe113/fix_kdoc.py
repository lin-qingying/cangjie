p = 'D:/code/intellij/cangjie/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt'
with open(p, 'rb') as f:
    t = f.read().decode('utf-8')

kdoc_cc = '''    /**
     * 运行 tower resolver 并规约候选集合。
     *
     * 规约顺序为 tower 收集、适用性分组、函数值 expected type 过滤、
     * overload-by-lambda 过滤，最终返回候选集合、适用性与转发诊断。
     */
'''
my_doc_start = '''    /**
     * CJMP 消费侧遮蔽（D1 的 `FilterOutCommonCandidatesIfSpecificExist` 对位）。'''

if t.count(kdoc_cc) == 1:
    i_cc = t.find(kdoc_cc)
    i_my = t.find(my_doc_start, i_cc)
    assert i_my != -1, 'my doc not found after collectCandidates KDoc'
    # 我的块：从 my_doc_start 到 "    private fun collectCandidates(" 之前
    i_fn = t.find('    private fun collectCandidates(', i_my)
    assert i_fn != -1
    my_block = t[i_my:i_fn].rstrip('\n')
    # 移除原位置（含其后的两个换行）
    t = t[:i_my] + t[i_fn:]
    # 在我的块之前重新放置：先放我的块，再放 collectCandidates KDoc
    t = t[:i_cc] + my_block + '\n\n' + kdoc_cc + t[i_cc + len(kdoc_cc):]
    with open(p, 'wb') as f:
        f.write(t.encode('utf-8'))
    print('kdoc ownership fixed')
else:
    print(f'skip: kdoc anchor count {t.count(kdoc_cc)}')
