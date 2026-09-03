@echo off
rem استاندارد اسکریپت راه‌انداز Gradle Wrapper برای ویندوز.
rem توجه: طبق README.md، فایل gradle-wrapper.jar باید یک‌بار ساخته شود.

set DIR=%~dp0
set CLASSPATH=%DIR%gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
    echo خطا: gradle-wrapper.jar پیدا نشد.
    echo لطفاً طبق بخش "نحوه Build" در README.md ابتدا Gradle Wrapper را بسازید:
    echo   gradle wrapper --gradle-version 8.7
    echo یا پروژه را در Android Studio باز کنید تا خودکار ساخته شود.
    exit /b 1
)

java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
