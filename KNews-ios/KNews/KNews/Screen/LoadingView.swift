import SwiftUI

struct LoadingView: View {
    var body: some View {
        VStack {
            Spacer()

            Text("Loading...").font(.body)

            Spacer().frame(height: 24)

            ProgressView().progressViewStyle(CircularProgressViewStyle())

            Spacer()
        }
    }
}

struct LoadingView_Previews: PreviewProvider {
    static var previews: some View {
        LoadingView()
    }
}
