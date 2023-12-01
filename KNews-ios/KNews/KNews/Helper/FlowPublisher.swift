import Foundation
import Combine
import HackerNews

class FlowPublisher<T> : Kotlinx_coroutines_coreFlowCollector {

    private let underlyingFlow: Kotlinx_coroutines_coreFlow

    private let subject: PassthroughSubject<T, Never>
    let values: AnyPublisher<T, Never>

    init(origin: Kotlinx_coroutines_coreFlow) {
        underlyingFlow = origin
        subject = PassthroughSubject<T, Never>()
        values = subject.eraseToAnyPublisher()

        underlyingFlow.collect(collector: self) { (err) in print(err) }
    }
    
    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        subject.send(value as! T)
        completionHandler(nil)
    }
}

extension Kotlinx_coroutines_coreFlow {

    func toAnyPublisher<T>() -> AnyPublisher<T, Never> {
        return FlowPublisher(origin: self).values
    }
}
