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
mvn -version >nul 2>&1
if %errorlevel% eq 0 (
    call mvn compile exec:java "-Dexec.mainClass=com.the.matrix.arsw.The_matrix_escape.TheMatrixEscapeApplication"
) else (
    call mvnw.cmd compile exec:java "-Dexec.mainClass=com.the.matrix.arsw.The_matrix_escape.TheMatrixEscapeApplication"
)

if %errorlevel% neq 0 (
    echo [ERROR] Execution failed. Check the output above.
)
pause



