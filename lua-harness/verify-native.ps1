#requires -Version 7.0
param([string]$JavaHome = $env:JAVA_HOME)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'native-common.ps1')
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:PATH
try {
    Set-HarnessToolchain $JavaHome
    $engineRoot = Split-Path -Parent $PSScriptRoot
    & (Join-Path $engineRoot 'mvnw.cmd') '-B' '-f' (Join-Path $PSScriptRoot 'pom.xml') '-Pnative' 'native:compile'
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    $runDirectory = Join-Path $PSScriptRoot ('target/lua-smoke-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $runDirectory | Out-Null
    $executable = Join-Path $runDirectory 'enginefx-lua-native-harness.exe'
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'target/enginefx-lua-native-harness.exe') -Destination $executable
    # Only this EXE and newly generated input files enter the otherwise empty directory.
    $external = Join-Path $runDirectory 'created-after-build.lua'
    [IO.File]::WriteAllText($external, "assert(utf8.len('a$([char]0xe7)$([char]0xe3)o') == 4)`nengine_record(84)`nfunction enginefx_external_value() return 84 end", [Text.UTF8Encoding]::new($false))
    $result = Invoke-HarnessExecutable $executable $runDirectory @($external)
    if ($result.ExitCode -ne 0 -or $result.Output -notmatch 'callback=84 javaToLua=42 hooks=\d+ external=true lifecycle=200') {
        throw 'Native Lua acceptance failed.'
    }

    $invalidCases = @(
        @{ Name = 'bytecode'; Bytes = [byte[]]@(0x1b, 0x4c, 0x75, 0x61); Diagnostic = 'Lua bytecode is not accepted' },
        @{ Name = 'utf8'; Bytes = [byte[]]@(0xc3, 0x28); Diagnostic = 'Lua source must be UTF-8' },
        @{ Name = 'no-callback'; Source = 'function enginefx_external_value() return 84 end'; Diagnostic = 'did not invoke engine_record' },
        @{ Name = 'wrong-return'; Source = 'engine_record(84); function enginefx_external_value() return 84.5 end'; Diagnostic = 'External Lua module did not execute' },
        @{ Name = 'protected-loop'; Source = 'while true do pcall(function() while true do end end) end'; Diagnostic = 'Lua instruction limit exceeded' },
        @{ Name = 'nested-loop'; Source = 'while true do pcall(function() xpcall(function() while true do end end, function(e) return e end) end) end'; Diagnostic = 'Lua instruction limit exceeded' }
    )
    foreach ($case in $invalidCases) {
        $inputFile = Join-Path $runDirectory ($case.Name + '.lua')
        if ($case.ContainsKey('Bytes')) { [IO.File]::WriteAllBytes($inputFile, $case.Bytes) }
        else { [IO.File]::WriteAllText($inputFile, $case.Source, [Text.UTF8Encoding]::new($false)) }
        $rejected = Invoke-HarnessExecutable $executable $runDirectory @($inputFile)
        if ($rejected.ExitCode -eq 0 -or $rejected.Output -notmatch [regex]::Escape($case.Diagnostic)) {
            throw ('Native rejection failed: ' + $case.Name)
        }
        Write-Host ('PASS rejection: ' + $case.Name)
    }
    Write-Host "PASS native Lua gate in $runDirectory"
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:PATH = $previousPath
}
exit 0
