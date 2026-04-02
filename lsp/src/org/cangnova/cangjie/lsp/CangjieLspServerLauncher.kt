package org.cangnova.cangjie.lsp

import org.cangnova.cangjie.lsp.server.CangjieLanguageServer
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import java.io.InputStream
import java.io.OutputStream

object CangjieLspServerLauncher {
    fun create(options: CangjieLspServerOptions = CangjieLspServerOptions()): CangjieLanguageServer {
        return CangjieLanguageServer(options)
    }

    fun launch(
        input: InputStream = System.`in`,
        output: OutputStream = System.out,
        options: CangjieLspServerOptions = CangjieLspServerOptions(),
    ): Launcher<LanguageClient> {
        val server = create(options)
        val launcher = LSPLauncher.createServerLauncher(server, input, output)
        server.connect(launcher.remoteProxy)
        launcher.startListening()
        return launcher
    }
}

fun main() {
    CangjieLspServerLauncher.launch()
}
