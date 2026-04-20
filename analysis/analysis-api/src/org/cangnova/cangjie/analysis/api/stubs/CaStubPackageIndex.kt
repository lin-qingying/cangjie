package org.cangnova.cangjie.analysis.api.stubs

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

interface CaStubPackageIndex {
    fun getAvailablePackages(): Set<FqName>

    fun getTopLevelClassifierNames(packageFqName: FqName): Set<Name>

    fun getTopLevelCallableNames(packageFqName: FqName): Set<Name>
}
