#requires -Version 7.0

function Set-HarnessToolchain([string]$JavaHome) {
    if (-not $JavaHome) {
        $nativeCommand = Get-Command native-image -ErrorAction Stop
        $JavaHome = Split-Path -Parent (Split-Path -Parent $nativeCommand.Source)
    }
    $JavaHome = (Resolve-Path -LiteralPath $JavaHome).Path
    $release = Get-Content -Raw -LiteralPath (Join-Path $JavaHome 'release')
    if ($release -notmatch '(?m)^GRAALVM_VERSION="25\.3\.4\.1"\r?$' -or
        $release -notmatch '(?m)^JAVA_VERSION="25\.' -or
        $release -notmatch '(?m)^OS_ARCH="x86_64"\r?$' -or
        -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/native-image.cmd'))) {
        throw 'This gate requires GraalVM 25.3.4.1, JDK 25, Windows x64 and Native Image.'
    }
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;$env:PATH"
    Write-Host "TOOLCHAIN $JavaHome"
}

function Invoke-HarnessExecutable([string]$Executable, [string]$WorkingDirectory, [string[]]$Arguments = @()) {
    $start = [Diagnostics.ProcessStartInfo]::new($Executable)
    $start.WorkingDirectory = $WorkingDirectory
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    foreach ($argument in $Arguments) { $start.ArgumentList.Add($argument) }
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $start
    try {
        if (-not $process.Start()) { throw 'Cannot start native harness.' }
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit(30000)) {
            $process.Kill($true)
            $process.WaitForExit()
            throw 'Native harness exceeded the 30 second deadline.'
        }
        $output = $stdout.GetAwaiter().GetResult()
        $errorOutput = $stderr.GetAwaiter().GetResult()
        if ($output) { Write-Host $output.TrimEnd() }
        if ($errorOutput) { Write-Host $errorOutput.TrimEnd() }
        return [pscustomobject]@{ ExitCode = $process.ExitCode; Output = $output + $errorOutput }
    } finally { $process.Dispose() }
}
