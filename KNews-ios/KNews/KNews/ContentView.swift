import SwiftUI
import HackerNews

struct ContentView: View {

    let service = HackerNewsServiceImpl(api: HackerNewsDependency().networkModule)

    var body: some View {
        Text("Hello, world!")
            .padding()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
