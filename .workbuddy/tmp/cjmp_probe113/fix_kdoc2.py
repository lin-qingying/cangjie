p = 'D:/code/intellij/cangjie/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt'
with open(p, 'rb') as f:
    t = f.read().decode('utf-8')

nl = '\r\n' if '\r\n' in t else '\n'

kdoc_cc = (
    '/**\n'
    '     * 运行 tower resolver 并规约候选集合。\n'
    '     *\n'
    '     * 规约顺序为 tower 收集、适用性分组、函数值 expected type 过滤、\n'
    '     * overload-by-lambda 过滤，最终返回候选集合、适用性与转发诊断。\n'
    '     */\n'
)
my_doc_head = '/**\n     * CJMP 消费侧遮蔽'
fn_sig = '    private fun collectCandidates(\n        info: CallInfo,'

# 以文件真实换行构造
kdoc_cc_n = '    ' + kdoc_cc.replace('\n', nl) if not kdoc_cc.startswith('    ') else kdoc_cc.replace('\n', nl)
# 去掉可能重复缩进
kdoc_cc_n = kdoc_cc_n.replace('        /**', '    /**', 1) if kdoc_cc_n.startswith('        ') else kdoc_cc_n
my_doc_head_n = ('    ' + my_doc_head.replace('\n', nl)) if not my_doc_head.startswith('    ') else my_doc_head.replace('\n', nl)
fn_sig_n = fn_sig.replace('\n', nl)

count_kdoc = t.count(kdoc_cc_n)
if count_kdoc == 1 and t.find(my_doc_head_n) > t.find(kdoc_cc_n):
    t = t.replace(kdoc_cc_n, '', 1)
    insert_at = t.find(fn_sig_n)
    assert insert_at != -1, 'collectCandidates signature not found'
    t = t[:insert_at] + kdoc_cc_n + t[insert_at:]
    with open(p, 'wb') as f:
        f.write(t.encode('utf-8'))
    print('kdoc reordered')
else:
    print(f'skip (count={count_kdoc})')
