import SwiftUI
import HackerNews

@main
struct KNewsApp: App {
    var body: some Scene {
        WindowGroup {
            KNewsListView(service: HackerNewsServiceImpl(api: HackerNewsDependency().networkModule))
        }
    }
}
