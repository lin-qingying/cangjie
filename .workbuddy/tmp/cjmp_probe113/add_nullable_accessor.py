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
    assert c == 1, f"{what}: anchor count {c} (lf={text.count(old)})"
    return text.replace(old2, new2, 1)


# 1) storage：增加可空访问器
p = R + '/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMappingStorage.kt'
t = read(p)
if 'cjmpMappingStorageOrNull' not in t:
    old = '''/** 当前 session 的 CJMP 配对结果存储（session factory 逐 session 注册）。 */
val CfirSession.cjmpMappingStorage: CfirCjmpMappingStorage by
    CfirSession.sessionComponentAccessor()'''
    new = '''/** 当前 session 的 CJMP 配对结果存储（session factory 逐 session 注册）。 */
val CfirSession.cjmpMappingStorage: CfirCjmpMappingStorage by
    CfirSession.sessionComponentAccessor()

/**
 * 当前 session 的 CJMP 配对结果存储（可空访问版本）。
 *
 * 供可能运行在未装配存储的轻量会话（如平台 common 会话、桩会话）内的消费点使用：
 * 缺失时按"无配对数据"处理，不抛出组件缺失错误。
 */
val CfirSession.cjmpMappingStorageOrNull: CfirCjmpMappingStorage? by
    CfirSession.nullableSessionComponentAccessor()'''
    t = replace_once(t, old, new, 'nullable accessor')
    t = replace_once(t, 'import org.cangnova.cangjie.cfir.session.sessionComponentAccessor\n',
                     'import org.cangnova.cangjie.cfir.session.nullableSessionComponentAccessor\n' if 'import org.cangnova.cangjie.cfir.session.sessionComponentAccessor\n' not in t else 'import org.cangnova.cangjie.cfir.session.nullableSessionComponentAccessor\n',
                     'nullable import') if False else t
    # 确保可空访问器扩展可用（与 CfirSession 同包，无需 import；此处仅确认 sessionComponentAccessor 调用形式）
    write(p, t)
    print('nullable accessor added')
else:
    print('nullable accessor exists')

# 2) CfirCallResolver：过滤点切换到可空访问器
p = R + '/cfir/entrypoint/../resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt'
p = R + '/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt'
t = read(p)
if 'cjmpMappingStorageOrNull' not in t:
    t = replace_once(t, 'import org.cangnova.cangjie.cfir.resolve.cjmp.cjmpMappingStorage',
                     'import org.cangnova.cangjie.cfir.resolve.cjmp.cjmpMappingStorageOrNull', 'filter import')
    old = '''        val storage = session.cjmpMappingStorage
        if (storage.isEmpty) return candidates'''
    new = '''        val storage = session.cjmpMappingStorageOrNull ?: return candidates
        if (storage.isEmpty) return candidates'''
    t = replace_once(t, old, new, 'filter storage access')
    write(p, t)
    print('call resolver switched to nullable')
else:
    print('call resolver already nullable')
