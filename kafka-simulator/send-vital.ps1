<#
.SYNOPSIS
    Convenience PowerShell script for triggering vitals simulation in MediSphere.
.DESCRIPTION
    Can publish directly to Kafka via the Node.js simulator or call the backend
    REST simulation endpoint: POST /api/vitals/simulate
.PARAMETER Mode
    Simulation mode: single, invalid, duplicate, unknown, stream, or rest. Default: single.
.PARAMETER PatientId
    Patient ID to simulate vitals for. Default: pat-001.
#>
param (
    [string]$Mode = "single",
    [string]$PatientId = "pat-001",
    [string]$Broker = "localhost:9092",
    [string]$BackendUrl = "http://localhost:8080"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " MediSphere Vitals Simulator Helper" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Mode:       $Mode"
Write-Host "Patient ID: $PatientId"

if ($Mode -eq "rest") {
    Write-Host "Authenticating as admin to obtain JWT..." -ForegroundColor Yellow
    $loginBody = @{
        username = "admin"
        password = "Admin@123"
    } | ConvertTo-Json

    try {
        $loginRes = Invoke-RestMethod -Uri "$BackendUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
        $token = $loginRes.token
        Write-Host "Obtained JWT token for admin." -ForegroundColor Green

        $vitalsBody = @{
            patientId = $PatientId
            deviceId = "POWERSHELL-SIM-01"
            heartRate = 72.0
            systolicBP = 120.0
            diastolicBP = 80.0
            oxygenSaturation = 98.5
            temperature = 36.8
            respiratoryRate = 16.0
            source = "SIMULATED"
        } | ConvertTo-Json

        $headers = @{
            Authorization = "Bearer $token"
        }

        Write-Host "Submitting simulation request to $BackendUrl/api/vitals/simulate..." -ForegroundColor Yellow
        $simRes = Invoke-RestMethod -Uri "$BackendUrl/api/vitals/simulate" -Method Post -Headers $headers -Body $vitalsBody -ContentType "application/json"
        Write-Host "Response:" -ForegroundColor Green
        $simRes | Format-List
    } catch {
        Write-Error "Failed to send via REST API: $_"
    }
} else {
    Write-Host "Executing Node.js Kafka simulator with mode '$Mode'..." -ForegroundColor Yellow
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    node "$scriptDir/simulator.js" "--mode=$Mode" "--patientId=$PatientId" "--broker=$Broker"
}
