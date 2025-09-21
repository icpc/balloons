@echo off

REM Usage:
REM   start: balloons.bat
REM   commands:
REM     balloons.bat volunteer create [--admin] login password
REM     balloons.bat volunteer update login --manage=true
REM     balloons.bat volunteer update login --new-password=password
REM     balloons.bat volunteer delete login
REM     balloons.bat reset-contest
REM     balloons.bat h2shell

SET PORT=8081
SET CONFIG_DIR=config
SET CREDS_FILE=creds.json
SET SELF_REGISTRATION=true

SET EXTRA_ARGS=
IF "%~1"=="" (
    SET EXTRA_ARGS=--port=%PORT%
    IF NOT "%SELF_REGISTRATION%"=="true" (
        SET EXTRA_ARGS=%EXTRA_ARGS% --disable-registration
    )
)

java -jar balloons.jar ^
    %EXTRA_ARGS% ^
    --config-directory=%CONFIG_DIR% ^
    --creds=%CREDS_FILE% ^
    %*
