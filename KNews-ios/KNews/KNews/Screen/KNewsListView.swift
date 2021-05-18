import SwiftUI
import HackerNews

struct KNewsListView: View {

    let service: HackerNewsService

    @ObservedObject var viewModel: HackerNewsListViewModelWrapper
    
    @State var sort = ListUiSortCondition.none

    init(service: HackerNewsService) {
        self.service = service
        self.viewModel = HackerNewsListViewModelWrapper(service: service)
    }

    var body: some View {
        let stories = viewModel.state.stories

        NavigationView {
            ZStack {
                if (stories.isLoading) {
                    LoadingView()
                } else if (stories.isSuccess) {
                    StoryListView(stories: stories.get() as! [ListUiRowState], onLoadNext: {
                        viewModel.loadNextStories()
                    })
                } else if (stories.isFailure) {
                    //do something with error
                }
            }
            .navigationBarTitle(Text("KNews"))
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Menu {
                        Picker(selection: $sort, label: Text("Sort")) {
                            Text("None").tag(ListUiSortCondition.none)
                            Text("Recent").tag(ListUiSortCondition.recent)
                            Text("Title").tag(ListUiSortCondition.title)
                            Text("Score").tag(ListUiSortCondition.score)
                        }
                        .onReceive([sort].publisher.first()) { newSort in
                            if (newSort == .none && viewModel.state.sortCondition != newSort) {
                                viewModel.loadStories()
                            }
                            viewModel.sortBy(sortCondition: newSort)
                        }
                    }
                    label: {
                        Label("Sort", systemImage: "arrow.up.arrow.down")
                    }
                }
            }
        }
        .onAppear {
            viewModel.loadStories()
        }
    }
}

struct StoryListView : View {

    let stories: [ListUiRowState]

    let onLoadNext: () -> Void

    var body: some View {
        List {
            ForEach(stories) { rowState in
                rowState.url.map {
                    NavigationLink(destination: KNewsDetailView(state: DetailUiStoryState(id: rowState.id, title: rowState.title, url: $0, commentIds: rowState.commentIds, descendants: rowState.descendants), service: HackerNewsServiceImpl(api: HackerNewsDependency().networkModule))) {
                        StoryRowView(state: rowState)
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }

            ProgressView().progressViewStyle(CircularProgressViewStyle())
                .modifier(CenterModifier())
                .onAppear {
                    onLoadNext()
                }
        }
    }
}

struct StoryRowView : View {

    let state: ListUiRowState

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("(\(state.url?.host ?? "null"))")
                .font(.caption)
                .padding(2)

            Text(state.title)
                .font(.system(size: 24))
                .padding(2)

            Text("by: \(state.by) | \(state.fromNowText)")
                .font(.system(size: 14))
        }
    }
}

extension ListUiRowState : Identifiable {}
