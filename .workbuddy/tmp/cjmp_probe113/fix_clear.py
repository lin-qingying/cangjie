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


p = R + '/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt'
t = read(p)

# 1) 移除 transformFile 中的 per-file clear（多文件模块会互相清空），改为相位开始前清一次
old_clear = '''    override fun transformFile(file: CfirFile, data: Nothing?): CfirFile {
        // 每轮匹配开始前重置存储，避免重复 resolve 残留（对齐 Kotlin 每轮重建 mapping 语义）
        storage.clear()
        return super.transformFile(file, data)
    }'''
new_clear = '''    override fun transformFile(file: CfirFile, data: Nothing?): CfirFile {
        return super.transformFile(file, data)
    }'''
t = replace_once(t, old_clear, new_clear, 'transformFile clear')

# 2) 处理器相位开始前清一次存储
old_processor = '''    override val transformer: CfirTransformer<Nothing?> = CfirCjmpMatcherTransformer(session, scopeSession)

    /**
     * 版本门关闭时整阶段早退（不遍历、不写存储，保持 1.0.x 行为零变化）。
     */'''
new_processor = '''    override val transformer: CfirTransformer<Nothing?> = CfirCjmpMatcherTransformer(session, scopeSession)

    /**
     * 相位开始前清空配对存储（每轮 CJMP_MATCHING 一次；多文件模块共享同一存储，
     * 不能在逐文件遍历中清空）。
     */
    override fun beforePhase() {
        super.beforePhase()
        if (cjmpEnabled) {
            session.cjmpMappingStorage.clear()
        }
    }

    /**
     * 版本门关闭时整阶段早退（不遍历、不写存储，保持 1.0.x 行为零变化）。
     */'''
t = replace_once(t, old_processor, new_processor, 'beforePhase clear')

write(p, t)
print('storage clear moved to beforePhase')
