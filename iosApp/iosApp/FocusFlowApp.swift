// Copyright 2026 IgnitronDev
// SPDX-License-Identifier: Apache-2.0

import Shared
import SwiftUI
import UIKit

/// The iOS app is a thin shell: the whole UI and logic live in the Kotlin Multiplatform
/// `shared` module, shown through `MainViewController()`.
@main
struct FocusFlowApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
