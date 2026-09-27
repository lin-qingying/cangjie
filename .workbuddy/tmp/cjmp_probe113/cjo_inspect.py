import struct, re, os, sys

R = 'D:/code/intellij/cangjie'
ATTR_KT = R + '/flatbuffers-gen/src/org/cangnova/cangjie/metadata/Attribute.kt'


def attr_names():
    """从生成的 Attribute.kt 读出枚举条目顺序（含显式数值时按显式值解析）。"""
    text = open(ATTR_KT, encoding='utf-8', errors='replace').read()
    body = text.split('enum class Attribute', 1)[1]
    body = body.split('{', 1)[1]
    depth = 1
    chunk = []
    for ch in body:
        if ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                break
        chunk.append(ch)
    body = ''.join(chunk)
    names = []
    next_value = 0
    for raw_line in body.splitlines():
        line = raw_line.split('//')[0].strip()
        if not line:
            continue
        m = re.match(r'^([A-Za-z_][A-Za-z0-9_]*)\(?\s*(\d+)?', line)
        if not m:
            continue
        name = m.group(1)
        explicit = m.group(2)
        if explicit is not None:
            value = int(explicit)
            while len(names) < value:
                names.append('<unnamed%d>' % len(names))
            names.append(name)
            next_value = value + 1
        else:
            names.append(name)
            next_value += 1
    return names


class FB:
    def __init__(self, data):
        self.d = data

    def u8(self, o):
        return self.d[o]

    def i8(self, o):
        return struct.unpack_from('<b', self.d, o)[0]

    def u16(self, o):
        return struct.unpack_from('<H', self.d, o)[0]

    def i32(self, o):
        return struct.unpack_from('<i', self.d, o)[0]

    def u32(self, o):
        return struct.unpack_from('<I', self.d, o)[0]

    def uoffset(self, o):
        """字段处 uoffset → 目标表绝对位置。"""
        return o + self.u32(o)

    def root(self):
        return self.uoffset(0)

    def vtable(self, table):
        return table - self.i32(table)

    def fields(self, table):
        vt = self.vtable(table)
        vt_size = self.u16(vt)
        n = (vt_size - 4) // 2
        return [self.u16(vt + 4 + 2 * i) for i in range(n)]

    def field(self, table, index):
        offs = self.fields(table)
        if index >= len(offs) or offs[index] == 0:
            return None
        return table + offs[index]

    def vector(self, o):
        if o is None:
            return []
        p = self.uoffset(o)
        n = self.u32(p)
        return [p + 4 + 4 * i for i in range(n)]

    def string(self, o):
        if o is None:
            return None
        p = self.uoffset(o)
        n = self.u32(p)
        return self.d[p + 4:p + 4 + n].decode('utf-8', 'replace')

    def indirect(self, o):
        return self.uoffset(o)


def dump_decl(fb, decl_pos, names):
    fields = fb.fields(decl_pos)
    identifier = fb.string(fb.field(decl_pos, 0))
    kind = fb.u16(fb.field(decl_pos, 1)) if fb.field(decl_pos, 1) else None
    # 找 attributes 字段：ulong 向量（我们的 schema: attributes:[ulong] 位于 decl 表内）
    attrs = None
    for idx in range(len(fields)):
        f = fb.field(decl_pos, idx)
        if f is None:
            continue
        # 猜测向量：目标位置处 n 合理且后续 8 字节向量
        target = fb.uoffset(f)
        if target + 4 <= len(fb.d):
            n = fb.u32(target)
            if 0 < n < 64 and target + 4 + 8 * n <= len(fb.d):
                vals = [struct.unpack_from('<Q', fb.d, target + 4 + 8 * i)[0] for i in range(n)]
                # 只接受"能在 Attribute 枚举范围内解释"的向量
                if any(v != 0 for v in vals):
                    bits = []
                    for w_i, w in enumerate(vals):
                        for b in range(64):
                            if w >> b & 1:
                                ordv = w_i * 64 + b
                                bits.append(names[ordv] if ordv < len(names) else '?%d' % ordv)
                    if attrs is None or len(bits) > len(attrs[1]):
                        attrs = (idx, bits)
    return identifier, kind, attrs, len(fields)


def inspect(path, limit=12):
    print('=' * 70)
    print('FILE', path, os.path.getsize(path), 'bytes')
    fb = FB(open(path, 'rb').read())
    root = fb.root()
    fields = fb.fields(root)
    print('Package table: %d vtable slots (fields), table size=%d, vtable size=%d'
          % (len(fields), fb.u16(fb.vtable(root) + 2), fb.u16(fb.vtable(root))))
    print('  present fields:', [(i, o) for i, o in enumerate(fields) if o])
    print('  fullPkgName =', fb.string(fb.field(root, 2)), ' moduleName =', fb.string(fb.field(root, 13)))
    print('  version =', fb.string(fb.field(root, 0)))
    cjo_ver = fb.field(root, 1)
    if cjo_ver:
        v = fb.indirect(cjo_ver)
        print('  cjoVersion = %d.%d.%d' % (fb.u8(v), fb.u8(v + 1), fb.u8(v + 2)))
    all_files = fb.vector(fb.field(root, 5))
    print('  allFiles =', [fb.string(p) for p in all_files][:6])
    file_info = fb.vector(fb.field(root, 14))
    print('  allFileInfo entries =', len(file_info))
    for p in file_info[:3]:
        t = fb.indirect(p)
        f = fb.fields(t)
        print('    FileInfo slots=%d present=%s' % (len(f), [i for i, o in enumerate(f) if o]))
    decls = fb.vector(fb.field(root, 8))
    print('  allDecls =', len(decls))
    names = ATTR_NAMES
    for p in decls[:limit]:
        t = fb.indirect(p)
        ident, kind, attrs, nfields = dump_decl(fb, t, names)
        print('    decl %-28s kind=%s slots=%d attrs=%s' % (ident, kind, nfields, attrs[1] if attrs else None))


ATTR_NAMES = attr_names()
print('Attribute ordinals of interest:',
      {n: i for i, n in enumerate(ATTR_NAMES) if n in
       ('COMMON', 'FROM_COMMON_PART', 'SPECIFIC', 'COMMON_WITH_DEFAULT', 'COMMON_NON_EXHAUSTIVE', 'GLOBAL')})

for f in ['full/cjmp_p.cjo', 'full/out2/cjmp_p.cjo']:
    p = R + '/.workbuddy/tmp/cjmp_probe113/' + f
    if os.path.exists(p):
        inspect(p)
    else:
        print('missing', p)
