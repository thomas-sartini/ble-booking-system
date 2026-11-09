import SwiftUI

/// Shared actions for a screen hosted in a NavigationStack.
/// Each screen supplies its own navigation title.
struct AppToolbar: ToolbarContent {
    var showMenu = true
    var enabled = true
    let onLogout: () -> Void

    var body: some ToolbarContent {
        if showMenu {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button(role: .destructive, action: onLogout) {
                        Label(
                            "logout",
                            systemImage: "rectangle.portrait.and.arrow.right"
                        )
                    }
                } label: {
                    Label("menu_options", systemImage: "ellipsis")
                }
                .disabled(!enabled)
            }
        }
    }
}

#Preview("App Toolbar") {
    NavigationStack {
        Color.clear
            .navigationTitle(Text("home_title"))
            .toolbar {
                AppToolbar(onLogout: {})
            }
    }
}
