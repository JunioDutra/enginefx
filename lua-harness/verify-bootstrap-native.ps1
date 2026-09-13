#requires -Version 7.0
param([string]$JavaHome = $env:JAVA_HOME)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'native-common.ps1')
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:PATH
try {
    Set-HarnessToolchain $JavaHome
    $engineRoot = Split-Path -Parent $PSScriptRoot
    $maven = Join-Path $engineRoot 'mvnw.cmd'
    & $maven '-B' '-f' (Join-Path $engineRoot 'pom.xml') 'install'
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $harnessPom = Join-Path $engineRoot 'bootstrap-harness/pom.xml'
    & $maven '-B' '-f' $harnessPom '-Pnative' 'native:compile'
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    $runDirectory = Join-Path $engineRoot ('bootstrap-harness/target/native-smoke-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $runDirectory | Out-Null
    $executable = Join-Path $runDirectory 'enginefx-native-bootstrap-harness.exe'
    Copy-Item -LiteralPath (Join-Path $engineRoot 'bootstrap-harness/target/enginefx-native-bootstrap-harness.exe') -Destination $executable
    $external = Join-Path $runDirectory 'created-after-build.lua'
    $source = @'
return {
  setup = function()
    assert(java == nil and io == nil and os == nil and debug == nil and require == nil)
    engine.state.set('ready', true)
  end,
  update = function(delta) engine.state.set('delta', delta) end,
  fixed_update = function(delta) engine.state.set('fixed', delta) end,
  on_event = function(kind, payload)
    assert(kind == 'host.tick')
    engine.state.set('event', payload)
  end,
  value = function() return 84 end,
  echo = function(value) return game.echo(value) end,
  dispose = function() engine.state.set('disposed', true) end
}
'@
    [IO.File]::WriteAllText($external, $source, [Text.UTF8Encoding]::new($false))
    $result = Invoke-HarnessExecutable $executable $runDirectory @($external)
    if ($result.ExitCode -ne 0 -or $result.Output -notmatch 'PASS native bootstrap: registry=true resources=true externalLua=true profile=NATIVE_JNI lifecycle=32 hostCallbacks=32 values=true') {
        throw 'Native bootstrap acceptance failed.'
    }
    $cases = @(
        @{ Name = 'wrong-value'; Source = $source.Replace('return 84', 'return 83'); Pattern = 'External Lua value mismatch' },
        @{ Name = 'protected-loop'; Source = $source.Replace('return 84', 'pcall(function() while true do end end); return 84'); Pattern = 'Lua instruction limit exceeded' },
        @{ Name = 'denied-capability'; Source = $source.Replace('return 84', "engine.events.emit('denied', {}); return 84"); Pattern = 'Missing script capability' },
        @{ Name = 'cyclic-value'; Source = $source.Replace('return game.echo(value)', 'local cyclic={}; cyclic.self=cyclic; return cyclic'); Pattern = 'cannot contain cycles' },
        @{ Name = 'invalid-utf8'; Bytes = [byte[]](255); Pattern = 'Lua source must be UTF-8' },
        @{ Name = 'bytecode'; Bytes = [byte[]](27,76,117,97); Pattern = 'Lua bytecode is not accepted' }
    )
    foreach ($case in $cases) {
        $inputFile = Join-Path $runDirectory ($case.Name + '.lua')
        if ($case.ContainsKey('Bytes')) { [IO.File]::WriteAllBytes($inputFile, $case.Bytes) }
        else { [IO.File]::WriteAllText($inputFile, $case.Source, [Text.UTF8Encoding]::new($false)) }
        $negative = Invoke-HarnessExecutable $executable $runDirectory @($inputFile)
        if ($negative.ExitCode -eq 0 -or $negative.Output -notmatch $case.Pattern) {
            throw "Native negative case failed: $($case.Name)"
        }
        Write-Host "PASS native rejection: $($case.Name)"
    }
    Write-Host "PASS native bootstrap gate in $runDirectory"
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:PATH = $previousPath
}
exit 0
