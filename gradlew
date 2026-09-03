#!/bin/sh

#
# استاندارد اسکریپت راه‌انداز Gradle Wrapper.
# توجه: فایل باینری gradle/wrapper/gradle-wrapper.jar به دلیل محدودیت محیط تولید این پروژه
# (نبود دسترسی اینترنت) همراه این پروژه نیست. طبق راهنمای README.md، اولین بار که پروژه
# را در Android Studio باز می‌کنید یا با دستور `gradle wrapper` آن را می‌سازید، این مشکل
# به‌طور خودکار برطرف می‌شود.
#

APP_HOME=$(cd "$(dirname "$0")" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
  echo "خطا: gradle-wrapper.jar پیدا نشد."
  echo "لطفاً طبق بخش «نحوه Build» در README.md ابتدا Gradle Wrapper را بسازید:"
  echo "  gradle wrapper --gradle-version 8.7"
  echo "یا پروژه را در Android Studio باز کنید تا خودکار ساخته شود."
  exit 1
fi

exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
