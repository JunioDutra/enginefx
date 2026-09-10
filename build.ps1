param([switch]$SkipTests)

$ErrorActionPreference = 'Stop'
if ($env:JAVA_HOME) {
    if (-not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
        throw 'JAVA_HOME must point to a JDK 25 or newer.'
    }
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Install JDK 25+ and set JAVA_HOME or add java to PATH.'
}
$buildArgs = @('-B', '-f', (Join-Path $PSScriptRoot 'pom.xml'), 'install')
if ($SkipTests) { $buildArgs += '-DskipTests' }
& "$PSScriptRoot\mvnw.cmd" @buildArgs
exit $LASTEXITCODE
