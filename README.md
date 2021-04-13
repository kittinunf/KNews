# KNews

The goal of this project is to build mobile apps that
consumes [HackerNews API](https://github.com/HackerNews/API) with Kotlin Multi-Platform technology.

## About

My idea is to build 2-page simple mobile applications (for both iOS and Android :) ). I planned to
make this simple enough so I can finish within a week and but at the same, it represents the
real-world app example that comprises of the quality of modern mobile app architecture which is
modular, scalable, testable and maintainable.

## Project structure

The project comprises of the following modules which aims to be modularized for easier to maintain
in the future.

The top level overview project structure is the following;

```
.
├── KNews-android
│ ├── build.gradle.kts
│ ├── libs
│ │ ├── hackernews-debug-xxx.aar (*)
│ │ └── hackernews-release-xxx.aar (*)
│ └── src
│     ├── androidTest
│     ├── main
│     └── test
├── KNews-ios
│ └── KNews
│     ├── KNews
│     └── KNews.xcodeproj
├── README.md
├── build.gradle.kts
├── libs
│ ├── hackernews
│ │   ├── build
│ |        └── bin
| |             └── ios     
│ |                  ├── HackerNews-debug.xcframework (*)
│ |                  └── HackerNews-release.xcframework (*)
│ │   ├── build.gradle.kts
│ │   └── src
│ └── redux
│     ├── build.gradle.kts
│     └── src
└── settings.gradle.kts
```

For the big picture, there are 2 main parts, the first one is the app modules (this includes both
iOS and Android app named [KNews-ios](KNews-ios) and [KNew-android](KNews-android), respectively).
The second one is the libs modules. Inside of the [libs](libs) modules contains 2 sub-modules.
Firstly, it is a [lib/redux](libs/redux)
which is the core architecture of the app. Secondly, there is the [lib/hackernews](libs/hackernews)
which is contain a domain-specific library which relates to HackerNews API.

(*) is the final artifact that will be consumed by the App which are `.aar` and `.xcframework` for
debug and release buildType that will be finally consumed by our mobile apps.

### App modules

Both app modules use modern/declarative/cutting-edge UI toolkit that is available at this moment to
develop the UI part. They are [Jetpack Compose](https://developer.android.com/jetpack/compose) for
building Android app (KNews-android) and [SwiftUI](https://developer.apple.com/xcode/swiftui/) for
iOS app (KNews-ios). They are simple yet powerful toolkit for driving modern apps' UI. Both is
backed by modern Kotlin and Swift programming languages.

### Library modules (Libs)

Libs module is a heart of the application where most of the logic reside. Even though this is such a
simple app with 2 screens (List & Detail). There are a little interesting bit of architectural
design inside due to the nature HackNews API where the API is quite barebones. Some of the
interesting things are, for example, the list is comprise of calling multiple APIs. One for getting
the feed items and multiple calls more by getting the detail of the items' id provided by the first
API. This is not the best API to consume but it is interesting and good amount of challenge to do
some business logic.

Due to the fact that we want to build both iOS & Android apps. It would be too-time consuming and
too much duplicate code if we were to write the "core" library part twice in 2 programming
languages. One in Swift and the other in Kotlin. Luckily, Kotlin has a multi-platform feature that
can transpile/compile the Kotlin code down to the native code for darwin. This is called a Kotlin
Multi Platform programming or [KMP](https://kotlinlang.org/docs/multiplatform.html). The core of
this app used this technique to be able to achieve the fact that we want to share the core part of
the app for both iOS and Android apps.

Libs module has 2 small sub modules which are both KMP-backed module. They
are [redux](libs/redux/README.md) and [hackernews](libs/hackernews/README.md).

### App Architecture

Heart and soul of the application lies in the library hackernews (`libs/hackernews`) which is power
by custom made/roll-by-my-own implementation of
the [Redux](https://redux.js.org/introduction/getting-started) architecture in Kotlin KMP
implementation. As we don't want to re-invent anything new, we follow closely the paradigm that is
proven to be working by the web front-end technology.

```none
┌──────────────┐    ┌──────────────┐
│ Android App  ├────|   iOS App    │       Application layer (UI) (Kotlin/Swift)
└───────▲──────┘    └──────▲───────┘
   aar* │                  │ framework*
┌──────────────────────────────────┐
│         HackerNews (libs)        │       Library layer (Core)
└──────────────────────────▲───────┘
                           │        
                    ┌──────┴───────┐
                    │ Redux (libs) │       Library layer (Redux)
                    └──────────────┘
```

Inside with our app, we use simple MVVM architecture where VM hold a single source of truth of the
screen and how the UI should look like. The output from the core layer is a single stream that of
the UI state that UI can subscribed to. This is represented and unfortunately need to be wrapped
twice with platform specific stream abstraction for the UI tool kit to be able to update UI
automatically.

#### MVVM

#### The ViewModel

* Android 🤖

The update is being abstracted with a class that represents a value over time
called ["State"](https://developer.android.com/jetpack/compose/state) which resides in our ViewModel
which lies in the code module libs/hackerNews (we can use it directly straight from the core lib).
Whenever the State object is updated with our UI state that is published from our core layer. The
Compose UI's State is updated, then the UI will be re-rendered accordingly.

Core's ViewModels (i.e. HackerNewsListViewModel and HackerNewsDetailViewModel)

```kotlin
class HackerNewsDetailViewModel(private val service: HackerNewsService) : NativeViewModel() {

    private val store: Store by lazy {
        createStore(
            scope = scope,
            initialState = DetailUiState(),
            reducer = DetailReducer(),
            middleware = DetailDataMiddleware(
                DetailEnvironment(
                    scope,
                    HackerNewsRepositoryImpl(service = service)
                ), detailUiStoryStateMapper, detailUiCommentRowStateMapper
            )
        )
    }

    @Suppress("Unused")
    val currentState
        get() = store.currentState

    val states = store.states
}
```

* UI for Android

```kotlin
val viewModel = viewModel<HackerNewsListViewModel>(factory = HackerNewsListViewModelFactory(service))
val states = viewModel.states.collectAsState()
val stories = states.stories

// update UI according to the states changes with compose UI
```

The `collectAsState()` is a Kotlin compose extension that convert the Coroutine's flow into the
State object that can be updated by Jetpack's Compose UI toolkit.

* iOS 🍎

The update is being abstracted with the help of Apple's reactive solution
called [Combine](https://developer.apple.com/documentation/combine) library. The key components that
make the Swift UI toolkit update the view tree accordingly are the `ObservableObject`
and `@Published`. This represent with a ViewModel wrapper layer (eg. for the list screen it
is `HackerNewsListViewModelWrapper`).

```swift
class HackerNewsListViewModelWrapper: ObservableObject {

    private let viewModel: HackerNewsListViewModel

    @Published var state: ListUiState

    private var cancellable: AnyCancellable?

    init(service: HackerNewsService) {
        viewModel = HackerNewsListViewModel(service: service)

        state = viewModel.currentState 

        cancellable = viewModel.states
            .toAnyPublisher()
            .assign(to: \.state, on: self)
    }
}
```
* UI for iOS
```swift
@ObservedObject var viewModel: HackerNewsListViewModelWrapper
let stories = viewModel.state.stories 

// Update the UI according to the states changes with SwiftUI
```

As you can see now that we have reaching the point where things are pretty similar on both iOS and
Android, we are publishing something that will change over time from the core. Then, we convert them
into the abstraction data type that can react to our UI toolkit (Jetpack Compose and SwiftUI)

To recap, this is the table represents what we are discussing so far.

```none
┌──────────────────────────────────┐
│            Application           │
└─────────────────▲────────────────┘
 @Published (iOS) │ StateFlow<T> (Android)
                  │
┌──────────────────────────────────┐
│  VM Wrapper for iOS and Android  │
└─────────────────▲────────────────┘
                  │
                  │ Flow<T>
┌──────────────────────────────────┐
│             ViewModel            │
└──────────────────────────────────┘
```

| Core | Android  | iOS |
| ---- | -------- | --- |
| [Flow<T>](https://kotlinlang.org/docs/flow.html) | [State<T>](https://developer.android.com/jetpack/compose/state) | [@ObservedObject](https://developer.apple.com/documentation/swiftui/observedobject) (consumer-side) and [@Publish](https://developer.apple.com/documentation/combine/published/) (producer-side) |

For the List screen specifically, the T type variable will be substituted with our defined "state"
object which is `ListUiState` which is hidden in our KMP (which is able to generate for iOS and
Android usage 🎉) module.

#### What's inside VM

In our VM, as our application grows managing state in a plain code could be very challenging. Our VM
is powered by predictable state handling strategy which is known as Redux. The idea behind Redux is
simple. Make the state changes as confined / specific as much as possible through the help of Redux
abstraction, eg. Reducer. We will be discussing about this more in detail inside
Redux's [README](libs/redux/README.md) file.

```none

                           ▲           ─┐
                           │            │ 
┌──────────────────────────┴───────┐    │ 
│             ViewModel            │    │     
└───────┬──────────────────▲───────┘    │     
        │                  │            │     KMP Module
┌───────▼──────┐    ┌──────┴───────┐    │  
│    Action    ├────▶    Store     │    │  
└──────────────┘    └──────────────┘    │ 
                                       ─┘
```

Inside our View is a Redux store, that specify how do we interact with our store with Action, then
we use the Action to mutate our State in a pure function called Reducer. In the reducer, we define
how our Action will change our State. If we want to interact with the external dependency, we can
use Middleware to do so.

## Dependencies

As one of the main goal of this project, we are trying to minimize the 3rd party dependencies used
in this project. However, the big ones that we couldn't avoid are the ones from Kotlin team like
Kotlin standard library and KotlinX (Kotlin extension).

### Core (Kotlin related)

- Kotlin standard library + Kotlinx Time
- Kotlin coroutines
- Kotlin serialization (for JSON Serialization/Deserialization)
- Ktor (Network libraries)

#### Android

- Ktor (for Android) with Okhttp for Networking
- MaterialDesign
- Core KTX (Kotlin extension)
- Jetpack compose
- AndroidX lifecycle related libraries

#### iOS

- Ktor (for iOS) with NSURLSession for Networking
- SwiftUI

## How to build

Just to simplify the build process and the way that we work with core libraries, we have created
Makefile so it is easier to build. This will also run tasks that compile, build and verify tests
then generate the final artifact for you.

For clean:

```shell
make clean
```

For building only core libraries:

```shell
make lib
```

For building for Android app:

```shell
make android
```

For install and launch Android app:

```shell
make install_android
```

Please make sure that you have `adb` install in your path. If you don't know how, please consult
this SO's [answer](https://stackoverflow.com/questions/17901692/set-up-adb-on-mac-os-x).

For building for iOS app:

```shell
make ios
```

The output will be something like the following, this means that the `.xcframework` is generated
correctly.

```shell
xcframework successfully written out to: KNews/libs/hackernews/build/bin/ios/HackerNews-debug.xcframework
xcframework successfully written out to: KNews/libs/hackernews/build/bin/ios/HackerNews-release.xcframework
```

In the case, you can't install the iOS app from the command line (either you don't want to or you don't have necessary xcode-install tool), please move to
the [.pbxproj](KNews-ios/KNews/KNews.xcodeproj), then you should be able to run the iOS on Xcode like usual.

For install iOS app: (with the command-line tool)

```shell
make install_ios
```

The `make install_ios` command is also build the app and install into the iPhone 11, iOS 14.4 as
described in the Makefile `-destination 'platform=iOS Simulator,name=iPhone 11,OS=14.4' \`. You will
need to launch the app by yourself however. Make sure that you have such simulator ready or the
installation will be failed.

### Android App

#### ListScreen

<img src="screenshots/android/ss1.png" width="25%" />
<img src="screenshots/android/ss2.png" width="25%" />
<img src="screenshots/android/ss3.png" width="25%" />

#### DetailScreen

<img src="screenshots/android/ss4.png" width="25%" />
<img src="screenshots/android/ss5.png" width="25%" />

### iOS App

#### ListScreen

<img src="screenshots/ios/ss1.png" width="25%" />
<img src="screenshots/ios/ss2.png" width="25%" />
<img src="screenshots/ios/ss3.png" width="25%" />

#### DetailScreen

<img src="screenshots/ios/ss4.png" width="25%" />
<img src="screenshots/ios/ss5.png" width="25%" />
