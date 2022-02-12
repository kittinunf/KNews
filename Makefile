NAME = KNews

clean:
	rm -rf KNews-ios/build
	./gradlew clean

all: android ios
install: install_android install_ios

lib:
	./gradlew :libs:hackernews:build

ios: lib
	./gradlew assembleHackerNewsXCFramework

install_ios : ios
	xcodebuild clean -project KNews-ios/KNews/KNews.xcodeproj -scheme KNews
	xcodebuild -project KNews-ios/KNews/KNews.xcodeproj \
	-scheme KNews \
	-configuration Debug \
	-destination 'platform=iOS Simulator,name=iPhone 11,OS=15.2' \
	-derivedDataPath KNews-ios/build
	xcrun simctl install \
	"iPhone 11" \
	./KNews-ios/build/Build/Products/Debug-iphonesimulator/KNews.app

android: lib
	./gradlew KNews-android:assembleDebug
	./gradlew KNews-android:installDebug

install_android: android
	adb -d install -r KNews-android/build/outputs/apk/debug/KNews-android-debug.apk
	adb shell am start -n com.github.kittinunf.app.knews/com.github.kittinunf.app.knews.MainActivity
