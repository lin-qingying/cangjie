import os
import re

R = 'D:/code/intellij/cangjie'
SRC = R + '/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMappingStorage.kt'
DST = R + '/cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpMappingStorage.kt'


def read(p):
    with open(p, 'rb') as f:
        return f.read().decode('utf-8')


def write(p, t):
    with open(p, 'wb') as f:
        f.write(t.encode('utf-8'))


# 1) 迁移文件：改包名，去掉跨包导入（CfirDeclaration/CfirSession/CfirSessionComponent 的导入路径保持有效）
t = read(SRC)
assert 'package org.cangnova.cangjie.cfir.resolve.cjmp' in t
t = t.replace('package org.cangnova.cangjie.cfir.resolve.cjmp',
              'package org.cangnova.cangjie.cfir.session', 1)
# 文档注明迁移原因
t = t.replace('''/**
 * specific 声明与 candidate common 声明不能配对的原因分类。''',
              '''/**
 * specific 声明与 candidate common 声明不能配对的原因分类。''', 1)
os.makedirs(os.path.dirname(DST), exist_ok=True)
write(DST, t)
os.remove(SRC)
print('storage moved to', DST)

# 2) 更新导入：全仓（cfir + analysis）中所有指向旧包的引用
IMPORT_FIXES = [
    ('import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMappingStorage',
     'import org.cangnova.cangjie.cfir.session.CfirCjmpMappingStorage'),
    ('import org.cangnova.cangjie.cfir.resolve.cjmp.cjmpMappingStorage',
     'import org.cangnova.cangjie.cfir.session.cjmpMappingStorage'),
    ('import org.cangnova.cangjie.cfir.resolve.cjmp.cjmpMappingStorageOrNull',
     'import org.cangnova.cangjie.cfir.session.cjmpMappingStorageOrNull'),
]
# 同包引用（resolve/cjmp 下的文件现在需要新增导入）
SAME_PKG_FILES = [
    R + '/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatchRunner.kt',
    R + '/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatcher.kt',
]
NEED_IMPORTS = {
    'CfirCjmpMappingStorage': 'import org.cangnova.cangjie.cfir.session.CfirCjmpMappingStorage',
    'CjmpMismatchKind': 'import org.cangnova.cangjie.cfir.session.CjmpMismatchKind',
}

root_dirs = [R + '/cfir', R + '/analysis']
changed = []
for base in root_dirs:
    for dirpath, dirnames, filenames in os.walk(base):
        dirnames[:] = [d for d in dirnames if d not in ('bin', 'build', '.git')]
        for fn in filenames:
            if not fn.endswith('.kt'):
                continue
            fp = os.path.join(dirpath, fn)
            if fp == DST:
                continue
            txt = read(fp)
            orig = txt
            for old, new in IMPORT_FIXES:
                if old in txt:
                    txt = txt.replace(old, new)
            write_needed = False
            for name, imp in NEED_IMPORTS.items():
                # 同包文件需要新增导入（该文件在 resolve/cjmp 下且使用了该类型，但缺少导入）
                if re.search(r'\b' + name + r'\b', txt) and imp not in txt and \
                        'package org.cangnova.cangjie.cfir.resolve.cjmp' in txt:
                    anchor = 'import org.cangnova.cangjie.cfir.session.CfirSession'
                    if anchor in txt:
                        txt = txt.replace(anchor, anchor + '\n' + imp, 1)
                        write_needed = True
            if txt != orig:
                write(fp, txt)
                changed.append(fp)

print('imports updated in %d files:' % len(changed))
for c in changed:
    print('  ', c.replace(R + '/', ''))
