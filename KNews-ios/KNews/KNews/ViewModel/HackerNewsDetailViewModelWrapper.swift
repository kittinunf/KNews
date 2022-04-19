import Foundation
import HackerNews
import Combine

class HackerNewsDetailViewModelWrapper: ObservableObject {

    private let viewModel: HackerNewsDetailViewModel

    @Published var state: DetailUiState

    private var cancellable: AnyCancellable?

    init(initialState: DetailUiState, service: HackerNewsService) {
        viewModel = HackerNewsDetailViewModel.Companion().create(state: initialState, service: service)
        
        state = viewModel.currentState

        cancellable = viewModel.states
            .toAnyPublisher()
            .assign(to: \.state, on: self)
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
