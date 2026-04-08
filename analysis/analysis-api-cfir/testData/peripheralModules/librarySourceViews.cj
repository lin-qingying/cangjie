// MODULE: librarySourceViews
// MODULE_KIND: LibrarySource
// MAIN_MODULE
// MAIN_FILE_NAME: librarySourceViews.cj
// FILE: librarySourceViews.cj
package sample.peripheral.library

class LibraryGreeter {
    func member(): Int64 {
        return exported()
    }
}

func exported(): Int64 {
    return 7
}
