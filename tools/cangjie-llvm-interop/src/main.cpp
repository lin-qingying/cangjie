#include <iostream>
#include <string>
#include <vector>

#include "llvm/Bitcode/BitcodeWriter.h"
#include "llvm/Config/llvm-config.h"
#include "llvm/IR/LLVMContext.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/Verifier.h"
#include "llvm/IRReader/IRReader.h"
#include "llvm/Support/CommandLine.h"
#include "llvm/Support/MemoryBuffer.h"
#include "llvm/Support/SourceMgr.h"
#include "llvm/Support/raw_ostream.h"

extern "C" {
#include "llvm-c/Core.h"
}

#if defined(_WIN32)
#include <fcntl.h>
#include <io.h>
#endif

namespace {
constexpr int EXIT_USAGE = 2;
constexpr int EXIT_RUNTIME = 3;

std::string EscapeJson(const std::string& value)
{
    std::string escaped;
    escaped.reserve(value.size());
    for (char ch : value) {
        switch (ch) {
            case '\\':
                escaped += "\\\\";
                break;
            case '\"':
                escaped += "\\\"";
                break;
            case '\n':
                escaped += "\\n";
                break;
            case '\r':
                escaped += "\\r";
                break;
            case '\t':
                escaped += "\\t";
                break;
            default:
                escaped.push_back(ch);
                break;
        }
    }
    return escaped;
}

void PrintProbeJson()
{
    // These symbols are checked by Kotlin-side backend health check.
    const std::vector<std::string> symbols = {
        "LLVMGetVersion",
        "LLVMContextCreate",
        "LLVMModuleCreateWithName",
        "LLVMPrintModuleToString",
    };

    // Touch the functions to ensure they are linked and callable in this process.
    (void)&LLVMGetVersion;
    (void)&LLVMContextCreate;
    (void)&LLVMModuleCreateWithName;
    (void)&LLVMPrintModuleToString;

    std::cout << "{";
    std::cout << "\"llvmVersion\":\"" << EscapeJson(LLVM_VERSION_STRING) << "\",";
    std::cout << "\"symbols\":[";
    for (size_t i = 0; i < symbols.size(); ++i) {
        if (i != 0) {
            std::cout << ",";
        }
        std::cout << "\"" << symbols[i] << "\"";
    }
    std::cout << "]";
    std::cout << "}";
    std::cout << std::endl;
}

int EmitBitcodeFromStdin(const std::string& moduleName)
{
#if defined(_WIN32)
    _setmode(_fileno(stdin), _O_BINARY);
    _setmode(_fileno(stdout), _O_BINARY);
#endif

    llvm::LLVMContext context;
    llvm::SMDiagnostic parseError;

    auto inputOrErr = llvm::MemoryBuffer::getSTDIN();
    if (!inputOrErr) {
        llvm::errs() << "failed to read llvm ir from stdin\n";
        return EXIT_RUNTIME;
    }

    std::unique_ptr<llvm::Module> module =
        llvm::parseIR((*inputOrErr)->getMemBufferRef(), parseError, context);
    if (!module) {
        llvm::errs() << "failed to parse llvm ir for module '" << moduleName << "'\n";
        parseError.print("cangjie-llvm-interop", llvm::errs());
        return EXIT_RUNTIME;
    }

    std::string verifyMessage;
    llvm::raw_string_ostream verifyStream(verifyMessage);
    if (llvm::verifyModule(*module, &verifyStream)) {
        verifyStream.flush();
        llvm::errs() << "invalid llvm module '" << moduleName << "':\n" << verifyMessage << "\n";
        return EXIT_RUNTIME;
    }

    llvm::WriteBitcodeToFile(*module, llvm::outs());
    llvm::outs().flush();
    return 0;
}

int PrintUsage(const char* argv0)
{
    llvm::errs() << "Usage:\n";
    llvm::errs() << "  " << argv0 << " probe --json\n";
    llvm::errs() << "  " << argv0 << " emit-bitcode --module <name>\n";
    return EXIT_USAGE;
}
} // namespace

int main(int argc, char** argv)
{
    if (argc < 2) {
        return PrintUsage(argv[0]);
    }

    const std::string command = argv[1];

    if (command == "probe") {
        if (argc != 3 || std::string(argv[2]) != "--json") {
            return PrintUsage(argv[0]);
        }
        PrintProbeJson();
        return 0;
    }

    if (command == "emit-bitcode") {
        if (argc != 4 || std::string(argv[2]) != "--module") {
            return PrintUsage(argv[0]);
        }
        const std::string moduleName = argv[3];
        return EmitBitcodeFromStdin(moduleName);
    }

    return PrintUsage(argv[0]);
}

