import Foundation
import HackerNews
import Combine

class HackerNewsDetailViewModelWrapper: ObservableObject {

    private let viewModel: HackerNewsDetailViewModel

    @Published var state: DetailUiState

    private var cancellable: AnyCancellable?

    init(service: HackerNewsService) {
        viewModel = HackerNewsDetailViewModel(service: service)
        
        state = viewModel.currentState

        cancellable = viewModel.states
            .toAnyPublisher()
            .assign(to: \.state, on: self)
    }

    func setInitialStory(state: DetailUiStoryState) {
        viewModel.setInitialStory(state: state)
    }

    func loadStory() {
        viewModel.loadStory()
    }

    func loadStoryComments() {
        viewModel.loadStoryComments()
    }

    deinit {
        viewModel.cancel()
    }
}
