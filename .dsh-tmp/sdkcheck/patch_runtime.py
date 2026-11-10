"""Patch libcangjie-runtime.dll so its module-list scan cannot dereference a bad name pointer.

At RVA 0x19EAA the runtime does `call wcslen` on a wchar_t* taken from the PEB module
list; on this OS build the newest LDR entry is not fully initialised (DllBase=0,
name buffer=0x620026), which makes wcslen fault.  Replacing the 5-byte call with
`xor eax,eax; nop; nop; nop` makes the module name length come out as 0 instead of
dereferencing the pointer.

Usage:
  python patch_runtime.py <dll> --dry-run
  python patch_runtime.py <dll> --apply
  python patch_runtime.py <dll> --restore <backup>
"""
import shutil
import struct
import sys

RVA = 0x19EAA
NEW_BYTES = bytes([0x31, 0xC0, 0x90, 0x90, 0x90])  # xor eax,eax ; nop ; nop ; nop


def rva_to_off(data, rva):
    e_lfanew = struct.unpack_from("<I", data, 0x3C)[0]
    coff = e_lfanew + 4
    nsec = struct.unpack_from("<H", data, coff + 2)[0]
    opt_size = struct.unpack_from("<H", data, coff + 16)[0]
    opt = coff + 20
    magic = struct.unpack_from("<H", data, opt)[0]
    sec = opt + opt_size
    for i in range(nsec):
        o = sec + i * 40
        vsize, vaddr, rsize, raddr = struct.unpack_from("<IIII", data, o + 8)
        if vaddr <= rva < vaddr + max(vsize, rsize):
            return raddr + (rva - vaddr)
    raise ValueError("rva not mapped: 0x%X" % rva)


def main():
    dll = sys.argv[1]
    mode = sys.argv[2]
    data = open(dll, "rb").read()
    off = rva_to_off(data, RVA)
    old = data[off:off + 5]
    print("image  : %s" % dll)
    print("rva    : 0x%X -> file offset 0x%X" % (RVA, off))
    print("before : %s" % old.hex(" "))
    if old[0] != 0xE8:
        print("ABORT: expected a 5-byte call (E8 ..) at this offset")
        return 1
    if mode == "--dry-run":
        print("after  : %s (dry run, nothing written)" % NEW_BYTES.hex(" "))
        return 0
    if mode == "--restore":
        backup = sys.argv[3]
        shutil.copyfile(backup, dll)
        print("restored from %s" % backup)
        return 0
    if mode == "--apply":
        with open(dll + ".bak", "wb") as fh:
            fh.write(data)
        with open(dll, "r+b") as fh:
            fh.seek(off)
            fh.write(NEW_BYTES)
        print("after  : %s" % NEW_BYTES.hex(" "))
        print("backup : %s.bak" % dll)
        return 0
    print("unknown mode: %s" % mode)
    return 2


if __name__ == "__main__":
    sys.exit(main())
