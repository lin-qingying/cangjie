"""打印 e2e fixture 的 期望/实际 差异（只输出不同的行）。用法：python diff_e2e.py [Psi]"""
import sys, re, html, glob, difflib
suffix = 'Diagnostics2PsiTestGenerated' if len(sys.argv) > 1 and sys.argv[1] == 'Psi' else 'Diagnostics2TestGenerated'
root = 'D:/code/intellij/cangjie/cfir/analysis-tests/build/test-results/test/'
files = [f for f in glob.glob(root + '*.xml') if suffix + '$CommonSpecific$E2e' in f]
for f in files:
    s = open(f, encoding='utf-8', errors='replace').read()
    for m in re.finditer(r'<testcase name="(\w+)\(\)"(.*?)</testcase>', s, re.S):
        body = m.group(2)
        fm = re.search(r'<failure message="(.*?)" type=', body, re.S)
        if not fm:
            continue
        msg = html.unescape(fm.group(1))
        parts = re.split(r'\n=====[^\n]*======\n', msg)
        print('#####', m.group(1))
        if len(parts) < 3:
            print(msg[:1500]); continue
        exp, act = parts[1].splitlines(), parts[2].splitlines()
        for line in difflib.unified_diff(exp, act, lineterm='', n=0):
            if line.startswith(('---', '+++')):
                continue
            print(line)
