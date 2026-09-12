#requires -Version 7.0
param([string]$JavaHome = $env:JAVA_HOME)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'native-common.ps1')
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:PATH
try {
    Set-HarnessToolchain $JavaHome
    $engineRoot = Split-Path -Parent $PSScriptRoot
    & (Join-Path $engineRoot 'mvnw.cmd') '-B' '-f' (Join-Path $PSScriptRoot 'pom.xml') '-Pnative-platform' 'native:compile'
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $runDirectory = Join-Path $PSScriptRoot ('target/platform-smoke-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $runDirectory | Out-Null
    $executable = Join-Path $runDirectory 'enginefx-native-platform-harness.exe'
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'target/enginefx-native-platform-harness.exe') -Destination $executable
    # Native Image may emit Java Sound's support DLL next to the executable.
    $audioLibrary = Join-Path $PSScriptRoot 'target/jsound.dll'
    if (Test-Path -LiteralPath $audioLibrary) { Copy-Item -LiteralPath $audioLibrary -Destination $runDirectory }
    $result = Invoke-HarnessExecutable $executable $runDirectory
    $runExitCode = $result.ExitCode
    Write-Host "PLATFORM exit=$runExitCode directory=$runDirectory"
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:PATH = $previousPath
}
exit $runExitCode
