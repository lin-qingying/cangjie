/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.lang.declarations

import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider



class CangJieFileViewProvider(
    manager: PsiManager,
    file: VirtualFile,
    physical: Boolean,
    private val factory: (CangJieFileViewProvider) -> CjFile?,
    private val textProvider: ((CangJieFileViewProvider) -> CharSequence)? = null,
) : SingleRootFileViewProvider(manager, file, physical, CangJieLanguage) {
    /**
     * 统一由外部工厂创建 source/decompiled `CjFile`，
     * 避免调用方再手工塞入 cached PSI。
     */
    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
        return factory(this)
    }

    override fun getContents(): CharSequence {
        return textProvider?.invoke(this) ?: super.getContents()
    }

    override fun createCopy(copy: VirtualFile) = CangJieFileViewProvider(manager, copy, false, factory, textProvider)


}
