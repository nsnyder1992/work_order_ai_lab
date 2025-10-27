@echo off
rem Start Tomcat in debug mode for remote debugging

echo Starting Tomcat in debug mode...
echo Debug port: 8000
echo.

cd /d "C:\work\vtomcat\bin"

rem Set JPDA environment variables for remote debugging
set JPDA_ADDRESS=8000
set JPDA_TRANSPORT=dt_socket

rem Start Tomcat with JPDA debugging enabled
call catalina.bat jpda start

echo.
echo Tomcat started in debug mode.
echo You can now attach your debugger to localhost:8000
pause