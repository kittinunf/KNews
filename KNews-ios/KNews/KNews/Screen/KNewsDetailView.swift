import SwiftUI
import HackerNews
import WebKit

struct KNewsDetailView: View {

    let state: DetailUiStoryState

    @ObservedObject var viewModel: HackerNewsDetailViewModelWrapper

    @State private var bottomSheetShown = false

    init(state: DetailUiStoryState, service: HackerNewsService) {
        self.state = state
        self.viewModel = HackerNewsDetailViewModelWrapper(service: service)
    }
    
    var body: some View {
        let comments = viewModel.state.comments

        ZStack {
            WebView(request: URLRequest(url: URL(string: state.url.description())!))
            GeometryReader { geo in
                BottomSheetView(isOpen: self.$bottomSheetShown, maxHeight: geo.size.height * 0.8, content: {
                    if (comments.isLoading) {
                        LoadingView()
                    } else if (comments.isSuccess) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Comments: \(state.commentIds?.count ?? 0)")
                                .font(.system(size: 14))
                                .padding(10)
                            CommentListView(comments: comments.get() as? [DetailUiCommentRowState] ?? [])
                        }
                    } else if (comments.isFailure) {
                        
                    }
                })
            }.edgesIgnoringSafeArea(.all)
        }
        .navigationBarTitle(Text(state.title))
        .onAppear {
            viewModel.setInitialStory(state: state)
            viewModel.loadStoryComments()
        }
    }
}

struct WebView : UIViewRepresentable {
    let request: URLRequest

    func makeUIView(context: Context) -> WKWebView {
        return WKWebView()
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        uiView.load(request)
    }
}

struct CommentListView : View {

    var comments: [DetailUiCommentRowState]

    var body: some View {
        List {
            ForEach(comments) { rowState in
                VStack(alignment: .leading, spacing: 8, content: {
                    Text("\(rowState.by) | \(rowState.fromNowText)")
                        .font(.system(size: 12))
                        .padding(4)

                    if (rowState.text.isEmpty) {
                        Text("( Comment is deleted )...").foregroundColor(.gray)
                    } else {
                        Text(rowState.text).font(.body).padding(4)
                    }
                })
            }
        }
    }
}

extension DetailUiCommentRowState : Identifiable {}
