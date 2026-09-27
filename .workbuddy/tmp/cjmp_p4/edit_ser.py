root = 'D:/code/intellij/cangjie/cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/'
s = ''


def load(p):
    global s
    s = open(p, encoding='utf-8', newline='').read()
    crlf = '\r\n' in s
    s = s.replace('\r\n', '\n')
    return crlf


def save(p, crlf):
    open(p, 'w', encoding='utf-8', newline='').write(s.replace('\n', '\r\n') if crlf else s)


def rep(old, new):
    global s
    assert s.count(old) == 1, old
    s = s.replace(old, new)


p = root + 'CjoPackageWriter.kt'
c = load(p)
rep('import PackageFormat.Option\nimport PackageFormat.StringId\n', 'import PackageFormat.CompilationOptions\n')
rep(' * identifiers[StringId].str`。', ' * identifiers[string]`（与官方 origin/main `CjoFormat.fbs` 字节布局一致）。')
rep('''                .map { identifier -> StringId.createStringId(builder, builder.createString(identifier)) }''',
    '''                .map(builder::createString)''')
rep('''    fun write(builder: FlatBufferBuilder): Int = Option.createOption(builder, debug, optLevel)''',
    '''    fun write(builder: FlatBufferBuilder): Int =
        CompilationOptions.createCompilationOptions(builder, optLevel, debug)''')
save(p, c)

p = root + 'CjoPackageHeader.kt'
c = load(p)
rep('''                CjoModuleOptionInfo(debug = option.debug, optLevel = option.optLevel)''',
    '''                CjoModuleOptionInfo(debug = option.debug, optLevel = option.optimizationLevel)''')
rep('''                                feature.identifiers(identifierIndex)?.str?.let(::add)''',
    '''                                feature.identifiers(identifierIndex)?.let(::add)''')
save(p, c)
print('ok')
