import SwiftUI

struct BottomSheetView<C: View> : View {
    @Binding var isOpen: Bool

    let minHeight: CGFloat
    let maxHeight: CGFloat

    let content: C

    @GestureState private var translation: CGFloat = 0

    private var offset: CGFloat {
        isOpen ? 0 : maxHeight - minHeight
    }

    private var indicator : some View {
        Image(systemName: "chevron.up")
            .onTapGesture {
                self.isOpen.toggle()
            }
    }

    init(isOpen: Binding<Bool>, maxHeight: CGFloat, @ViewBuilder content: () -> C) {
        self.minHeight = maxHeight * 0.1
        self.maxHeight = maxHeight
        self.content = content()
        self._isOpen = isOpen
    }

    var body: some View {
        GeometryReader { geo in
            VStack(spacing: 0) {
                self.indicator.padding(8)
                self.content
            }
            .frame(width: geo.size.width, height: self.maxHeight, alignment: .top)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(16)
            .frame(height: geo.size.height, alignment: .bottom)
            .offset(y: max(self.offset + self.translation, 0))
            .animation(.interactiveSpring())
            .gesture(
                DragGesture().updating(self.$translation, body: { (value, state, _) in
                    state = value.translation.height
                }).onEnded { value in
                    let snapDistance = self.maxHeight * 0.25

                    guard abs(value.translation.height) > snapDistance else {
                        return
                    }

                    self.isOpen = value.translation.height < 0
                }
            )
        }
    }
}
