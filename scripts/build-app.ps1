$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot

try {
    Write-Host "Installing frontend dependencies..."
    & npm --prefix frontend ci
    if ($LASTEXITCODE -ne 0) {
        throw "npm ci failed."
    }

    Write-Host "Building React frontend..."
    & npm --prefix frontend run build
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend build failed."
    }

    Write-Host "Running Maven tests and packaging..."
    & mvn package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed. Stop any running JAR and retry."
    }

    $jarPath = Join-Path $projectRoot `
        "target\legal-contract-analyzer-0.1.0-SNAPSHOT-exec.jar"
    $frontendIndex = Join-Path $projectRoot "frontend\dist\index.html"

    if (-not (Test-Path $jarPath)) {
        throw "Executable JAR was not created: $jarPath"
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($jarPath)

    try {
        $entry = $archive.GetEntry(
            "BOOT-INF/classes/static/index.html"
        )

        if ($null -eq $entry) {
            throw "React index.html is missing from the JAR."
        }

        $stream = $entry.Open()
        $hasher = [System.Security.Cryptography.SHA256]::Create()

        try {
            $jarIndexHash = [BitConverter]::ToString(
                $hasher.ComputeHash($stream)
            ).Replace("-", "")
        }
        finally {
            $hasher.Dispose()
            $stream.Dispose()
        }
    }
    finally {
        $archive.Dispose()
    }

    $frontendIndexHash = (Get-FileHash `
        -Path $frontendIndex `
        -Algorithm SHA256).Hash

    if ($jarIndexHash -ne $frontendIndexHash) {
        throw "JAR contains an older React index.html."
    }

    Write-Host "SUCCESS: tests passed and current React build is in the executable JAR."
    Write-Host $jarPath
}
finally {
    Pop-Location
}