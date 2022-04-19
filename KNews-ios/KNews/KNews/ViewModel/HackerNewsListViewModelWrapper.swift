import Foundation
import HackerNews
import Combine

class HackerNewsListViewModelWrapper: ObservableObject {

    private let viewModel: HackerNewsListViewModel

    @Published var state: ListUiState

    private var cancellable: AnyCancellable?

    init(service: HackerNewsService) {
        viewModel = HackerNewsListViewModel(scope: , service: service)

        state = viewModel.currentState 

        cancellable = viewModel.states
            .toAnyPublisher()
            .assign(to: \.state, on: self)
    }

    func loadStories() {
        viewModel.loadStories()
    }

    func loadNextStories() {
        viewModel.loadNextStories()
    }

    func sortBy(sortCondition: ListUiSortCondition) {
        if (sortCondition != state.sortCondition && sortCondition == ListUiSortCondition.none) {
            viewModel.loadStories()
        }
        viewModel.sortBy(sortCondition: sortCondition)
    }

    deinit {
        viewModel.cancel()
    }
}
