@echo off
echo Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not configured in your system PATH.
    echo Please install JDK 21 or higher.
    pause
    exit /b
)

echo Running Matrix Escape...
call mvn -version >nul 2>&1
if %errorlevel%==0 (
    cmd /c "mvn compile exec:java -Dexec.mainClass=com.the.matrix.arsw.The_matrix_escape.TheMatrixEscapeApplication"
) else (
    cmd /c "mvnw.cmd compile exec:java -Dexec.mainClass=com.the.matrix.arsw.The_matrix_escape.TheMatrixEscapeApplication"
)

pause



