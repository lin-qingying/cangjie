import struct, sys

PATH = r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\runtime\lib\windows_x86_64_cjnative\libcangjie-runtime.dll"
data = open(PATH, "rb").read()

e_lfanew = struct.unpack_from("<I", data, 0x3C)[0]
assert data[e_lfanew:e_lfanew+4] == b"PE\0\0"
coff = e_lfanew + 4
machine, nsec, tds, symptr, nsym, optsize, chars = struct.unpack_from("<HHIIIHH", data, coff)
opt = coff + 20
magic = struct.unpack_from("<H", data, opt)[0]
pe32p = magic == 0x20B
image_base = struct.unpack_from("<Q", data, opt + 24)[0] if pe32p else struct.unpack_from("<I", data, opt + 28)[0]
dd = opt + (112 if pe32p else 96)
imp_rva, imp_size = struct.unpack_from("<II", data, dd + 8)
print(f"image_base=0x{image_base:X} imports_rva=0x{imp_rva:X}")

sec_off = opt + optsize
sections = []
for i in range(nsec):
    o = sec_off + i * 40
    name = data[o:o+8].rstrip(b"\0").decode("ascii", "replace")
    vsize, vaddr, rsize, raddr = struct.unpack_from("<IIII", data, o + 8)
    sections.append((name, vaddr, vsize, raddr, rsize))

def rva2off(rva):
    for name, vaddr, vsize, raddr, rsize in sections:
        if vaddr <= rva < vaddr + max(vsize, rsize):
            return raddr + (rva - vaddr)
    raise ValueError(f"rva 0x{rva:X} not mapped")

def cstr(rva):
    off = rva2off(rva)
    end = data.index(b"\0", off)
    return data[off:end].decode("ascii", "replace")

# walk import descriptors
thunks = {}
o = rva2off(imp_rva)
while True:
    oft, tds_, fc, name_rva, ft = struct.unpack_from("<IIIII", data, o)
    if oft == 0 and name_rva == 0 and ft == 0:
        break
    dll = cstr(name_rva)
    lookup = oft or ft
    i = 0
    lo = rva2off(lookup)
    while True:
        val = struct.unpack_from("<Q", data, lo + i * 8)[0]
        if val == 0:
            break
        if val & 0x8000000000000000:
            name = f"ordinal#{val & 0xFFFF}"
        else:
            name = cstr((val & 0x7FFFFFFF) + 2)
        thunks.setdefault(dll, []).append((name, image_base + ft + i * 8))
        i += 1
    o += 20

for dll, fs in thunks.items():
    print(f"\n== {dll} ({len(fs)}) ==")
    for name, iat in fs:
        if name in ("wcslen", "wcscmp", "wcscpy", "memset", "memcpy"):
            print(f"  {name:24s} IAT 0x{iat:016X}")

if len(sys.argv) > 1:
    print("\n== strings ==")
    for a in sys.argv[1:]:
        rva = int(a, 16)
        print(f"0x{rva:X}: {cstr(rva)!r}")
