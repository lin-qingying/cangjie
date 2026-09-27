R = 'D:/code/intellij/cangjie'


def read(p):
    with open(p, 'rb') as f:
        return f.read().decode('utf-8')


def write(p, t):
    with open(p, 'wb') as f:
        f.write(t.encode('utf-8'))


def replace_once(text, old, new, what):
    if text.count(old) == 1:
        return text.replace(old, new, 1)
    nl = '\r\n' if '\r\n' in text else '\n'
    old2, new2 = old.replace('\n', nl), new.replace('\n', nl)
    c = text.count(old2)
    assert c == 1, f"{what}: anchor count {c}"
    return text.replace(old2, new2, 1)


# 1) ComponentsContainers：公共组件注册点加入 CJMP 存储（覆盖全部会话种类）
p = R + '/cfir/entrypoint/src/org/cangnova/cangjie/cfir/session/ComponentsContainers.kt'
t = read(p)
if 'CfirCjmpMappingStorage' not in t:
    imp_anchor = 'import org.cangnova.cangjie.cfir.session.CfirExceptionHandler'
    if imp_anchor not in t:
        # 退而求其次：插到文件内最后一个 import 之后
        lines = t.split('\n')
        idxs = [i for i, l in enumerate(lines) if l.startswith('import ')]
        assert idxs, 'no imports'
        lines.insert(idxs[-1] + 1, 'import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMappingStorage')
        t = '\n'.join(lines)
    else:
        t = t.replace(imp_anchor, imp_anchor + '\nimport org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMappingStorage', 1)

    old = '''    // 注册扩展服务组件，管理编译器插件注册的扩展点（checker、生成器等）
    register(CfirExtensionService::class, CfirExtensionService())
}'''
    new = '''    // 注册扩展服务组件，管理编译器插件注册的扩展点（checker、生成器等）
    register(CfirExtensionService::class, CfirExtensionService())
    // 注册 CJMP（common/specific）配对结果存储：每个 session 独立实例（可变组件，不能共享默认单例；
    // 覆盖 CLI/LL/库/内置全部会话种类——specific 会话写入配对结果，common/库会话保持空存储）
    register(CfirCjmpMappingStorage::class, CfirCjmpMappingStorage())
}'''
    t = replace_once(t, old, new, 'common components')
    write(p, t)
    print('ComponentsContainers patched')
else:
    print('ComponentsContainers already patched')

# 2) CfirDefaultSessionFactory：撤销早先两处注册（收口到公共注册点）
p = R + '/cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirDefaultSessionFactory.kt'
t = read(p)
removed = 0
for old in [
    '''        // CJMP 配对结果存储：每个 session 独立实例（可变组件，不能使用共享默认单例）
        register(CfirCjmpMappingStorage::class, CfirCjmpMappingStorage())
''',
    '''        register(CfirCjmpMappingStorage::class, CfirCjmpMappingStorage())
''',
]:
    if t.count(old) >= 1:
        t = t.replace(old, '', 1)
        removed += 1
if 'CfirCjmpMappingStorage' not in t:
    imp = 'import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMappingStorage\n'
    if imp in t:
        t = t.replace(imp, '', 1)
write(p, t)
print('CfirDefaultSessionFactory cleaned:', removed)
