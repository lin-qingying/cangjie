package org.cangnova.cangjie.analysis.api.stubs

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind

interface CaStubFileProvider {
    fun getFileStubKind(file: CjFile): CangJieFileStubKind?

    fun getTopLevelClassifierNames(file: CjFile): Set<Name>

    fun getTopLevelCallableNames(file: CjFile): Set<Name>
}
