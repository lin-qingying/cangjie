// FILE: publicPackageDecl.cj
public package pkgmodpublic

public class PublicPkgMarked {
}

// FILE: publicPackageUse.cj
public package pkgmodpublic

func usePublicPkgMarked(): PublicPkgMarked {
    return PublicPkgMarked()
}

// FILE: protectedPackageDecl.cj
protected package pkgmodprotected

protected class ProtectedPkgMarked {
}

// FILE: protectedPackageUse.cj
protected package pkgmodprotected

func useProtectedPkgMarked(): ProtectedPkgMarked {
    return ProtectedPkgMarked()
}

// FILE: internalPackageDecl.cj
internal package pkgmodinternal

internal class InternalPkgMarked {
}

// FILE: internalPackageUse.cj
internal package pkgmodinternal

func useInternalPkgMarked(): InternalPkgMarked {
    return InternalPkgMarked()
}

// FILE: privatePackage.cj
// 官方 `Parse/ParserModifierRules.cpp` 的 `PACKAGE_MODIFIERS` 只列 public/internal/protected，
// 包声明不接受 private；该形态在解析期报 `parse_illegal_modifier_in_scope`，
// CFIR 侧的对应诊断为 WRONG_MODIFIER_TARGET。
<!WRONG_MODIFIER_TARGET!>private<!> package pkgmodprivate

class PrivatePkgMarked {
}
