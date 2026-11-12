param([ValidateSet('doctor','build','run','db-start','db-stop')][string]$Action='doctor')
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$tools='C:\Users\ASUS\Documents\Codex\toolchains'
$config=Get-Content -LiteralPath (Join-Path $tools 'd1mstay-toolchain.json') -Raw | ConvertFrom-Json
$env:JAVA_HOME=$config.JavaHome
$env:MAVEN_HOME=$config.MavenHome
$env:Path="$($config.JavaHome)\bin;$($config.MavenHome)\bin;$($config.PostgresBin);$env:Path"
$data=Join-Path $config.DatabaseRoot 'data'
function Start-Database {
    & "$($config.PostgresBin)\pg_ctl.exe" -D $data status *> $null
    if($LASTEXITCODE -ne 0){
        & "$($config.PostgresBin)\pg_ctl.exe" -D $data -l (Join-Path $config.DatabaseRoot 'postgres.log') -o '-h 127.0.0.1 -p 5433' -w start
        if($LASTEXITCODE -ne 0){throw 'Database D1MSTAY tidak dapat dimulai.'}
    }
}
if($Action -eq 'db-stop'){
    & "$($config.PostgresBin)\pg_ctl.exe" -D $data -m fast -w stop
    exit $LASTEXITCODE
}
Start-Database
if($Action -eq 'db-start'){exit 0}
$secret=Get-Content -LiteralPath (Join-Path $config.DatabaseRoot 'credentials.json') -Raw | ConvertFrom-Json
$env:D1MSTAY_DB_URL='jdbc:postgresql://127.0.0.1:5433/d1mstay_praktikum'
$env:D1MSTAY_DB_USER='d1mstay_app'
$env:D1MSTAY_DB_PASSWORD=$secret.AppPassword
Push-Location $root
try{
    switch($Action){
        'doctor'{
            & java -version
            & javac -version
            & "$($config.MavenHome)\bin\mvn.cmd" -version
            $env:PGPASSWORD=$secret.AppPassword
            & "$($config.PostgresBin)\psql.exe" -h 127.0.0.1 -p 5433 -U d1mstay_app -d d1mstay_praktikum --no-password -tAc 'SELECT current_database(), current_user;'
            $env:PGPASSWORD=$null
        }
        'build'{& "$root\mvnw.cmd" --batch-mode clean verify}
        'run'{& "$root\mvnw.cmd" --batch-mode spring-boot:run}
    }
    if($LASTEXITCODE -ne 0){throw "Perintah $Action gagal dengan kode $LASTEXITCODE"}
}finally{
    $env:D1MSTAY_DB_PASSWORD=$null
    Pop-Location
}
