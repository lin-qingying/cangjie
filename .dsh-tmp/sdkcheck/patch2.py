"""Second patch attempt: skip PEB module-list entries whose DllBase is 0.

The runtime walks PEB->Ldr->InMemoryOrderModuleList and calls wcslen on
BaseDllName.Buffer of every entry.  On this OS build the newest entry is linked
into the list before it is fully initialised (DllBase=0, SizeOfImage=0, name
buffer = garbage), so wcslen faults.

Original code at RVA 0x19E9D:
    4D 8B 76 50          mov  r14,[r12+50h]     ; BaseDllName.Buffer
    4D 85 F6             test r14,r14
    74 E9                je   <loop continue>
    4C 89 F1             mov  rcx,r14
    E8 ..                call wcslen

Replaced by (same 13 bytes, call site untouched):
    49 83 7E 20 00       cmp  qword ptr [r12+20h],0   ; DllBase
    74 EC                je   <loop continue>
    49 8B 4E 50          mov  rcx,[r12+50h]
    90 90                nop  nop

Usage: python patch2.py <dll> --dry-run | --apply | --restore <orig>
"""
import shutil
import struct
import sys

RVA = 0x19E9D
EXPECTED = bytes.fromhex("4D 8B 74 24 50 4D 85 F6 74 E9")
# cmp qword [r12+20h],0 ; je -0x14 ; mov rcx,[r12+50h] ; nop nop
NEW_BYTES = bytes.fromhex("49 83 7C 24 20 00 74 EB 49 8B 4C 24 50")


def rva_to_off(data, rva):
    e = struct.unpack_from("<I", data, 0x3C)[0]
    coff = e + 4
    nsec = struct.unpack_from("<H", data, coff + 2)[0]
    opt_size = struct.unpack_from("<H", data, coff + 16)[0]
    opt = coff + 20
    sec = opt + opt_size
    for i in range(nsec):
        o = sec + i * 40
        vsize, vaddr, rsize, raddr = struct.unpack_from("<IIII", data, o + 8)
        if vaddr <= rva < vaddr + max(vsize, rsize):
            return raddr + (rva - vaddr)
    raise ValueError("rva not mapped")


def main():
    dll, mode = sys.argv[1], sys.argv[2]
    data = open(dll, "rb").read()
    off = rva_to_off(data, RVA)
    old = data[off:off + 13]
    print("offset : 0x%X" % off)
    print("before : %s" % old.hex(" "))
    if old[:10] != EXPECTED:
        print("ABORT: unexpected original bytes")
        return 1
    if mode == "--dry-run":
        print("after  : %s" % NEW_BYTES.hex(" "))
        return 0
    if mode == "--restore":
        shutil.copyfile(sys.argv[3], dll)
        print("restored")
        return 0
    shutil.copyfile(dll, dll + ".bak")
    with open(dll, "r+b") as fh:
        fh.seek(off)
        fh.write(NEW_BYTES)
    print("after  : %s" % NEW_BYTES.hex(" "))
    print("backup : %s.bak" % dll)
    return 0


if __name__ == "__main__":
    sys.exit(main())

